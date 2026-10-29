param([string]$ComposeProject = 'mamxanh-dev')

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    $demoRoot = 'app/mamxanh-backend/src/main/resources/db/demo'
    $recipeSeed = Get-Content -LiteralPath "$demoRoot/R__demo_sample_data_46_recipe_seasonings.sql" -Raw -Encoding utf8
    $imageSeed = Get-Content -LiteralPath "$demoRoot/R__demo_sample_data_12_ingredient_images.sql" -Raw -Encoding utf8
    $conversionSeed = Get-Content -LiteralPath "$demoRoot/R__demo_sample_data_14_ingredient_unit_conversions.sql" -Raw -Encoding utf8
    $recipeJsonMatch = [regex]::Match($recipeSeed, "(?s)DECLARE @recipe_json NVARCHAR\(MAX\) = N'(\[.*?\])';\s*INSERT INTO @recipes")
    if (-not $recipeJsonMatch.Success) { throw 'Could not find the JSON recipe fixture in the recipe seed.' }
    $recipes = @($recipeJsonMatch.Groups[1].Value.Replace("''", "'") | ConvertFrom-Json)
    $titles = @($recipes | ForEach-Object { $_.title })
    if ($titles.Count -ne 54) { throw 'Expected exactly 54 named fixture recipes.' }
    $titleValues = ($titles | ForEach-Object { "(N'$($_.Replace("'", "''"))')" }) -join ",`n"
    $imageRows = [regex]::Matches($imageSeed, "\(N'((?:''|[^'])+)', N'(/ingredients/[^']+\.webp)'\)")
    if ($imageRows.Count -ne 209) { throw 'Expected exactly 209 ingredient image mappings.' }
    $imageNames = ($imageRows | ForEach-Object { "(N'$($_.Groups[1].Value)')" }) -join ",`n"
    $conversionRows = [regex]::Matches($conversionSeed, "(?m)^\s*\(N'((?:''|[^'])+)',\s*N'((?:''|[^'])+)',\s*([0-9]+(?:\.[0-9]+)?),\s*([01])\)[,;]?\s*$")
    if ($conversionRows.Count -lt 1) { throw 'Expected ingredient-specific demo unit conversions.' }
    $conversionKeys = @{}
    $demoIngredientNames = @{}
    foreach ($row in $imageRows) { $demoIngredientNames[$row.Groups[1].Value] = $true }
    $allowedUnitCodes = @('ml', 'L', 'tsp', 'tbsp', 'cup', 'quả', 'củ', 'miếng')
    $conversionValues = foreach ($row in $conversionRows) {
        $ingredientName = $row.Groups[1].Value
        $unitCode = $row.Groups[2].Value
        $grams = [decimal]::Parse($row.Groups[3].Value, [Globalization.CultureInfo]::InvariantCulture)
        if (-not $demoIngredientNames.ContainsKey($ingredientName)) { throw "Conversion references a non-demo ingredient: $ingredientName" }
        if ($unitCode -notin $allowedUnitCodes) { throw "Conversion uses a non-volume/count unit: $unitCode" }
        if ($grams -le 0) { throw "Conversion must be positive: $ingredientName/$unitCode" }
        $key = "$ingredientName|$unitCode"
        if ($conversionKeys.ContainsKey($key)) { throw "Duplicate conversion mapping: $key" }
        $conversionKeys[$key] = $true
        "(N'$ingredientName', N'$unitCode', $($row.Groups[3].Value), $($row.Groups[4].Value))"
    }
    $conversionValues = $conversionValues -join ",`n"
    foreach ($row in $imageRows) {
        $asset = Join-Path 'app/mamxanh-frontend/public' $row.Groups[2].Value.TrimStart('/')
        if (-not (Test-Path -LiteralPath $asset -PathType Leaf)) { throw "Missing asset: $asset" }
        $bytes = [System.IO.File]::ReadAllBytes((Join-Path $repoRoot $asset))
        if ($bytes.Length -lt 12 -or [System.Text.Encoding]::ASCII.GetString($bytes, 0, 4) -ne 'RIFF' -or [System.Text.Encoding]::ASCII.GetString($bytes, 8, 4) -ne 'WEBP') {
            throw "Invalid WebP asset: $asset"
        }
    }

    # The outer transaction rolls back sentinels, deliberate unit changes and image changes.
    # The seed's own BEGIN/COMMIT pairs nest within it; GO only resets variable scope.
    $setup = @"
SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
CREATE TABLE #titles (title NVARCHAR(120) PRIMARY KEY);
INSERT INTO #titles VALUES $titleValues;
CREATE TABLE #imageNames (name NVARCHAR(200) PRIMARY KEY);
INSERT INTO #imageNames VALUES $imageNames;
CREATE TABLE #expectedConversions (
    ingredient_name NVARCHAR(200) NOT NULL,
    unit_code NVARCHAR(50) NOT NULL,
    grams_per_unit DECIMAL(18,9) NOT NULL,
    is_approximate BIT NOT NULL,
    PRIMARY KEY (ingredient_name, unit_code)
);
INSERT INTO #expectedConversions VALUES $conversionValues;
SELECT * INTO #volumeBaseConversions FROM #expectedConversions WHERE unit_code = N'ml';
INSERT INTO #expectedConversions (ingredient_name, unit_code, grams_per_unit, is_approximate)
SELECT base.ingredient_name, unit.code, base.grams_per_unit * unit.base_factor, base.is_approximate
FROM #volumeBaseConversions base
JOIN [UNIT] unit ON unit.code IN (N'L', N'tsp', N'tbsp', N'cup') AND unit.dimension = N'VOLUME';
IF (SELECT COUNT(*) FROM [UNIT] WHERE code IN (N'ml', N'L', N'tsp', N'tbsp', N'cup') AND dimension = N'VOLUME') <> 5
    THROW 51126, 'All five standardized volume units are required for demo conversion checks.', 1;
SELECT post.recipe_id INTO #targets FROM [RECIPE_POST] post JOIN #titles t ON t.title = post.title
WHERE post.author_id = (SELECT user_id FROM [USER] WHERE email = 'demo-expert@mamxanh.local');
IF (SELECT COUNT(*) FROM #targets) <> 54 THROW 51100, 'Missing or ambiguous fixture posts.', 1;

-- Same title under another author and an unlisted title under the fixture author.
DECLARE @fixtureAuthor BIGINT = (SELECT user_id FROM [USER] WHERE email = 'demo-expert@mamxanh.local');
DECLARE @otherAuthor BIGINT = (SELECT user_id FROM [USER] WHERE email = 'demo-expert-02@mamxanh.local');
IF @otherAuthor IS NULL THROW 51101, 'Second demo expert is required for ownership checks.', 1;
INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type, difficulty, servings, prep_time_min, cook_time_min)
VALUES (@otherAuthor, N'Rau muống xào tỏi', N'Keep this other author recipe unchanged.', 'STIR_FRY', 'VEGAN', 'EASY', 3, 5, 7),
       (@fixtureAuthor, N'Fixture safety sentinel', N'Keep this non-fixture recipe unchanged.', 'STIR_FRY', 'VEGAN', 'EASY', 4, 5, 7);
INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
SELECT post.recipe_id, ingredient.ingredient_id, unit.unit_id, 2
FROM [RECIPE_POST] post CROSS JOIN [INGREDIENT] ingredient CROSS JOIN [UNIT] unit
WHERE ((post.author_id = @otherAuthor AND post.title = N'Rau muống xào tỏi') OR (post.author_id = @fixtureAuthor AND post.title = N'Fixture safety sentinel'))
AND ingredient.name = N'Tỏi' AND ingredient.status = 'ACTIVE' AND unit.code = N'nhánh';
SELECT * INTO #otherPosts FROM [RECIPE_POST] WHERE recipe_id NOT IN (SELECT recipe_id FROM #targets);
SELECT * INTO #otherIngredients FROM [RECIPE_INGREDIENT] WHERE recipe_id NOT IN (SELECT recipe_id FROM #targets);

-- Exercise an ingredient-specific conversion: 3 cloves -> 9 g before the curated upsert.
UPDATE ri SET quantity = 3, unit_id = (SELECT unit_id FROM [UNIT] WHERE code = N'nhánh')
FROM [RECIPE_INGREDIENT] ri JOIN [RECIPE_POST] post ON post.recipe_id = ri.recipe_id
JOIN [INGREDIENT] ingredient ON ingredient.ingredient_id = ri.ingredient_id
WHERE post.author_id = @fixtureAuthor AND post.title = N'Rau muống xào tỏi' AND ingredient.name = N'Tỏi';

-- This extra reference is outside the curated pairs: only the real conversion
-- can produce 12 g, so the canonical garlic upsert cannot hide a conversion bug.
INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
SELECT post.recipe_id, ingredient.ingredient_id, unit.unit_id, 4
FROM [RECIPE_POST] post CROSS JOIN [INGREDIENT] ingredient CROSS JOIN [UNIT] unit
WHERE post.author_id = @fixtureAuthor AND post.title = N'Khoai lang hấp'
  AND ingredient.name = N'Tỏi' AND ingredient.status = 'ACTIVE' AND unit.code = N'nhánh';

-- One Admin URL must survive; another empty image must be filled by the image seed.
UPDATE [INGREDIENT] SET image_url = N'/test-preserved-admin-upload.webp' WHERE name = N'Muối ăn' AND status = 'ACTIVE';
UPDATE [INGREDIENT] SET image_url = NULL WHERE name = N'Tỏi' AND status = 'ACTIVE';
SELECT ingredient_id, image_url INTO #existingImages FROM [INGREDIENT] WHERE NULLIF(LTRIM(RTRIM(image_url)), N'') IS NOT NULL;

-- A deliberately different oil conversion proves the local fixture never overwrites Admin data.
DECLARE @oilId BIGINT = (SELECT ingredient_id FROM [INGREDIENT] WHERE name = N'Dầu ăn');
DECLARE @mlId INT = (SELECT unit_id FROM [UNIT] WHERE code = N'ml');
IF @oilId IS NULL OR @mlId IS NULL THROW 51120, 'Oil and ml are required for conversion preservation checks.', 1;
IF NOT EXISTS (SELECT 1 FROM [INGREDIENT_UNIT_CONVERSION] WHERE ingredient_id = @oilId AND unit_id = @mlId)
    INSERT INTO [INGREDIENT_UNIT_CONVERSION] (ingredient_id, unit_id, grams_per_unit, is_approximate, is_active)
    VALUES (@oilId, @mlId, 0.950000000, 0, 1);
SELECT * INTO #initialConversions FROM [INGREDIENT_UNIT_CONVERSION];
GO
"@
    $afterFirst = @'
SELECT * INTO #firstIngredients FROM [RECIPE_INGREDIENT] WHERE recipe_id IN (SELECT recipe_id FROM #targets);
SELECT * INTO #firstPosts FROM [RECIPE_POST] WHERE recipe_id IN (SELECT recipe_id FROM #targets);
SELECT ingredient_id, image_url, updated_at INTO #firstImages FROM [INGREDIENT];
SELECT * INTO #firstConversions FROM [INGREDIENT_UNIT_CONVERSION];
GO
'@
    $checks = @'
IF EXISTS (SELECT * FROM #firstIngredients EXCEPT SELECT * FROM [RECIPE_INGREDIENT] WHERE recipe_id IN (SELECT recipe_id FROM #targets))
 OR EXISTS (SELECT * FROM [RECIPE_INGREDIENT] WHERE recipe_id IN (SELECT recipe_id FROM #targets) EXCEPT SELECT * FROM #firstIngredients)
 THROW 51102, 'Ingredient seed is not idempotent.', 1;
IF EXISTS (SELECT * FROM #firstPosts EXCEPT SELECT * FROM [RECIPE_POST] WHERE recipe_id IN (SELECT recipe_id FROM #targets))
 OR EXISTS (SELECT * FROM [RECIPE_POST] WHERE recipe_id IN (SELECT recipe_id FROM #targets) EXCEPT SELECT * FROM #firstPosts)
 THROW 51103, 'Recipe seed is not idempotent.', 1;
IF EXISTS (SELECT * FROM #firstImages EXCEPT SELECT ingredient_id, image_url, updated_at FROM [INGREDIENT])
 THROW 51104, 'Image seed is not idempotent.', 1;
IF EXISTS (SELECT * FROM #firstConversions EXCEPT SELECT * FROM [INGREDIENT_UNIT_CONVERSION])
 OR EXISTS (SELECT * FROM [INGREDIENT_UNIT_CONVERSION] EXCEPT SELECT * FROM #firstConversions)
 THROW 51121, 'Unit conversion seed is not idempotent.', 1;
IF EXISTS (SELECT * FROM #initialConversions EXCEPT SELECT * FROM [INGREDIENT_UNIT_CONVERSION])
 THROW 51122, 'Unit conversion seed changed an existing Admin conversion.', 1;
IF EXISTS (
    SELECT 1 FROM #expectedConversions expected
    JOIN [INGREDIENT] ingredient ON ingredient.name = expected.ingredient_name
    JOIN [UNIT] unit ON unit.code = expected.unit_code
    LEFT JOIN [INGREDIENT_UNIT_CONVERSION] conversion
      ON conversion.ingredient_id = ingredient.ingredient_id AND conversion.unit_id = unit.unit_id
    WHERE conversion.ingredient_id IS NULL OR conversion.grams_per_unit <= 0
       OR unit.dimension NOT IN (N'VOLUME', N'COUNT')
)
 THROW 51123, 'A curated ingredient-specific volume/count conversion is missing or invalid.', 1;
IF EXISTS (
    SELECT 1 FROM #expectedConversions expected
    JOIN [INGREDIENT] ingredient ON ingredient.name = expected.ingredient_name
    JOIN [UNIT] unit ON unit.code = expected.unit_code
    JOIN [INGREDIENT_UNIT_CONVERSION] conversion
      ON conversion.ingredient_id = ingredient.ingredient_id AND conversion.unit_id = unit.unit_id
    LEFT JOIN #initialConversions existing
      ON existing.ingredient_id = conversion.ingredient_id AND existing.unit_id = conversion.unit_id
    WHERE existing.ingredient_id IS NULL
      AND (conversion.grams_per_unit <> expected.grams_per_unit
           OR conversion.is_approximate <> expected.is_approximate OR conversion.is_active <> 1)
)
 THROW 51127, 'A newly inserted conversion differs from the curated demo value.', 1;
IF NOT EXISTS (
    SELECT 1 FROM [INGREDIENT_UNIT_CONVERSION] conversion
    JOIN [INGREDIENT] ingredient ON ingredient.ingredient_id = conversion.ingredient_id
    JOIN [UNIT] unit ON unit.unit_id = conversion.unit_id
    WHERE ingredient.name = N'Dầu ăn' AND unit.code = N'ml'
      AND conversion.grams_per_unit = 0.950000000 AND conversion.is_approximate = 0 AND conversion.is_active = 1
)
 THROW 51125, 'The Admin conversion sentinel was not preserved.', 1;
IF EXISTS (SELECT * FROM #otherPosts EXCEPT SELECT * FROM [RECIPE_POST] WHERE recipe_id NOT IN (SELECT recipe_id FROM #targets))
 OR EXISTS (SELECT * FROM [RECIPE_POST] WHERE recipe_id NOT IN (SELECT recipe_id FROM #targets) EXCEPT SELECT * FROM #otherPosts)
 THROW 51105, 'A non-fixture post was changed.', 1;
IF EXISTS (SELECT * FROM #otherIngredients EXCEPT SELECT * FROM [RECIPE_INGREDIENT] WHERE recipe_id NOT IN (SELECT recipe_id FROM #targets))
 OR EXISTS (SELECT * FROM [RECIPE_INGREDIENT] WHERE recipe_id NOT IN (SELECT recipe_id FROM #targets) EXCEPT SELECT * FROM #otherIngredients)
 THROW 51106, 'A non-fixture ingredient was changed.', 1;
IF EXISTS (SELECT 1 FROM #targets t WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] ri WHERE ri.recipe_id = t.recipe_id))
 THROW 51107, 'A fixture has no ingredients.', 1;
IF EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] ri JOIN #targets t ON t.recipe_id = ri.recipe_id JOIN [UNIT] unit ON unit.unit_id = ri.unit_id WHERE unit.code <> N'g' OR ri.quantity <= 0)
 THROW 51108, 'Fixture quantities must be positive grams.', 1;
IF EXISTS (SELECT ri.recipe_id, ri.ingredient_id FROM [RECIPE_INGREDIENT] ri JOIN #targets t ON t.recipe_id = ri.recipe_id GROUP BY ri.recipe_id, ri.ingredient_id HAVING COUNT(*) > 1)
 THROW 51109, 'A fixture has duplicate ingredient references.', 1;
IF EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] ri JOIN #targets t ON t.recipe_id = ri.recipe_id JOIN [INGREDIENT] i ON i.ingredient_id = ri.ingredient_id
 WHERE i.energy_kcal_100g IS NULL OR i.protein_g_100g IS NULL OR i.carbohydrate_g_100g IS NULL OR i.total_fat_g_100g IS NULL OR i.fiber_g_100g IS NULL OR i.calcium_mg_100g IS NULL OR i.iron_mg_100g IS NULL OR i.vitamin_b12_mcg_100g IS NULL OR i.zinc_mg_100g IS NULL)
 THROW 51110, 'Fixture ingredient nutrition is incomplete.', 1;
IF EXISTS (SELECT 1 FROM [INGREDIENT] i JOIN #imageNames names ON names.name = i.name
 WHERE i.energy_kcal_100g IS NULL OR i.protein_g_100g IS NULL OR i.carbohydrate_g_100g IS NULL OR i.total_fat_g_100g IS NULL OR i.fiber_g_100g IS NULL OR i.calcium_mg_100g IS NULL OR i.iron_mg_100g IS NULL OR i.vitamin_b12_mcg_100g IS NULL OR i.zinc_mg_100g IS NULL)
 THROW 51118, 'Demo catalog nutrition is incomplete.', 1;
IF EXISTS (SELECT 1 FROM [INGREDIENT] i JOIN #imageNames names ON names.name = i.name WHERE i.nutrition_supported = 0)
 THROW 51119, 'Stored demo nutrients are unexpectedly excluded from calculation.', 1;
IF EXISTS (SELECT ingredient_id, image_url FROM #existingImages EXCEPT SELECT ingredient_id, image_url FROM [INGREDIENT])
 THROW 51111, 'An existing ingredient image was replaced.', 1;
IF EXISTS (SELECT 1 FROM [INGREDIENT] i JOIN #imageNames names ON names.name = i.name WHERE NULLIF(LTRIM(RTRIM(i.image_url)), N'') IS NULL)
 THROW 51112, 'A named demo catalog ingredient still has no image.', 1;
IF NOT EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] ri JOIN [RECIPE_POST] post ON post.recipe_id = ri.recipe_id JOIN [INGREDIENT] i ON i.ingredient_id = ri.ingredient_id JOIN [UNIT] u ON u.unit_id = ri.unit_id
 WHERE post.author_id = (SELECT user_id FROM [USER] WHERE email = 'demo-expert@mamxanh.local') AND post.title = N'Rau muống xào tỏi' AND i.name = N'Tỏi' AND ri.quantity = 9 AND u.code = N'g')
 THROW 51113, 'Garlic clove conversion failed.', 1;
IF NOT EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] ri JOIN [RECIPE_POST] post ON post.recipe_id = ri.recipe_id JOIN [INGREDIENT] i ON i.ingredient_id = ri.ingredient_id JOIN [UNIT] u ON u.unit_id = ri.unit_id
 WHERE post.author_id = (SELECT user_id FROM [USER] WHERE email = 'demo-expert@mamxanh.local') AND post.title = N'Khoai lang hấp' AND i.name = N'Tỏi' AND ri.quantity = 12 AND u.code = N'g')
 THROW 51117, 'An additional fixture ingredient was not preserved with its gram conversion.', 1;
SELECT 'PASS' AS result, (SELECT COUNT(*) FROM #targets) AS recipes,
 (SELECT COUNT(*) FROM #firstIngredients) AS ingredient_references,
 (SELECT COUNT(*) FROM [INGREDIENT] i JOIN #imageNames names ON names.name = i.name) AS illustrated_ingredients,
 (SELECT COUNT(*) FROM #expectedConversions) AS expected_unit_conversions;
ROLLBACK TRANSACTION;
GO
'@
    $escapedRecipeSeed = $recipeSeed.Replace("'", "''")
    $negativeChecks = @"
BEGIN TRY
    BEGIN TRANSACTION;
    INSERT INTO [RECIPE_INGREDIENT] (recipe_id, custom_ingredient_name, unit_id, quantity)
    SELECT TOP (1) post.recipe_id, N'Fixture unknown density', unit.unit_id, 1
    FROM [RECIPE_POST] post CROSS JOIN [UNIT] unit
    WHERE post.author_id = (SELECT user_id FROM [USER] WHERE email = 'demo-expert@mamxanh.local')
      AND post.title = N'Rau muống xào tỏi' AND unit.code = N'ml';
    IF @@ROWCOUNT <> 1 THROW 51114, 'Cannot prepare unknown-conversion check.', 1;
    EXEC sys.sp_executesql N'$escapedRecipeSeed';
    THROW 51115, 'Unknown density should have stopped the seed.', 1;
END TRY
BEGIN CATCH
    DECLARE @actualError INT = ERROR_NUMBER();
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    IF @actualError <> 51003 THROW;
    PRINT 'PASS: unknown gram conversion is rejected and rolled back.';
END CATCH;
GO
BEGIN TRY
    BEGIN TRANSACTION;
    UPDATE [INGREDIENT] SET status = 'INACTIVE' WHERE name = N'Tỏi' AND status = 'ACTIVE';
    EXEC sys.sp_executesql N'$escapedRecipeSeed';
    THROW 51116, 'Missing active ingredient should have stopped the seed.', 1;
END TRY
BEGIN CATCH
    DECLARE @actualError INT = ERROR_NUMBER();
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    IF @actualError <> 51002 THROW;
    PRINT 'PASS: missing active ingredient is rejected and rolled back.';
END CATCH;
GO
"@
    $sql = $setup + "`n" + $imageSeed + "`nGO`n" + $conversionSeed + "`nGO`n" + $recipeSeed + "`nGO`n" + $afterFirst + "`n" + $imageSeed + "`nGO`n" + $conversionSeed + "`nGO`n" + $recipeSeed + "`nGO`n" + $checks + "`n" + $negativeChecks
    $oldEncoding = [Console]::OutputEncoding
    $oldPipeEncoding = $OutputEncoding
    try {
        [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
        $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
        $sql | & docker compose -p $ComposeProject --env-file app/mamxanh-backend/.env exec -T sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -d MamXanhDB -b -r 1 -f 65001
        if ($LASTEXITCODE -ne 0) { throw 'Demo ingredient SQL verification failed; the test transaction was rolled back on disconnect.' }
    } finally { [Console]::OutputEncoding = $oldEncoding; $OutputEncoding = $oldPipeEncoding }
} finally { Pop-Location }
