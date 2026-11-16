> **Document:** Thiết kế cơ sở dữ liệu hiện hành
> **File:** `docs/architecture/DATABASE-DESIGN-CURRENT.md`
> **Version:** v1.0.0
> **Created:** 2026-11-12
> **Last Updated:** 2026-11-12
> **Status:** Active

# Thiết kế cơ sở dữ liệu hiện hành

Tài liệu này mô tả phạm vi schema được triển khai để phân biệt schema hiện hành với ERD core baseline. Nó bổ sung thông tin phạm vi cho SDS; không thay thế migration, JPA entities, `database/schema.sql`, hoặc Data Dictionary được bảo vệ.

## Nguồn và phạm vi

- SQL Server là relational source of truth của ứng dụng. Backend sử dụng Spring Data JPA/Hibernate để ánh xạ entity và truy cập dữ liệu.
- Flyway là nguồn migration có thể thực thi; thư mục migration hiện đạt version `V44`. Các migration đã chia sẻ là append-only.
- `database/schema.sql` là snapshot bootstrap đồng bộ thủ công. Snapshot hiện chứa 35 tên bảng vật lý duy nhất, được nhóm theo domain dưới đây. Con số này là inventory của các khai báo bảng trong snapshot, không phải số migration và cũng không khẳng định mọi môi trường runtime đã áp dụng thành công.
- Logical/Physical ERD và Data Dictionary trong `docs/diagrams/ERD/` mô tả core baseline 22 bảng, chủ yếu đối chiếu V1/V2. Chúng không phải inventory đầy đủ của các extension về sau. Các diagram đó được bảo vệ và không được agent chỉnh sửa trong nhiệm vụ này.

## Inventory theo domain

| Domain | Bảng trong snapshot | Ghi chú thiết kế |
|---|---|---|
| Identity, community, moderation | `USER`, `USER_FOLLOW`, `USER_INGREDIENT_PREFERENCE`, `EXPERT_APPLICATION`, `USER_ACCOUNT_AUDIT`, `NOTIFICATION`, `REPORT`, `REPORT_SUPPLEMENT` | Danh tính và quan hệ cộng đồng/moderation được lưu bằng relational keys; Backend kiểm tra role, trạng thái tài khoản và quyền sở hữu. |
| Recipe, ingredients, discovery | `RECIPE_POST`, `RECIPE_MEDIA`, `RECIPE_INGREDIENT`, `INGREDIENT`, `UNIT`, `INGREDIENT_UNIT_CONVERSION`, `RECIPE_REACTION`, `RECIPE_VIEW`, `SAVED_RECIPE`, `COMMENT` | Recipe ingredients reference ingredient/unit records; recipe media and member interactions are related records. |
| Meal planning and shopping | `MEAL_PLAN`, `MEAL_PLAN_ENTRY`, `SHOPPING_LIST`, `SHOPPING_LIST_ITEM`, `SHOPPING_RECIPE_CONTRIBUTION` | Meal-plan entries refer to recipes. Shopping contributions retain recipe and, where available, meal-plan entry provenance for created list items. |
| Nutrition and commerce | `NUTRITION_DAILY_VALUE`, `SUBSCRIPTION`, `PAYMENT_TRANSACTION` | Nutrition reference data and subscription/payment records extend the core business model. |
| AI usage, credits, and action records | `AI_USAGE_TELEMETRY`, `AI_TOKEN_USAGE_LEDGER`, `AI_PLAN_CONFIGURATION`, `AI_GEMINI_MODEL_CONFIGURATION`, `AI_CREDIT_CONFIGURATION`, `AI_CREDIT_WALLET`, `AI_CREDIT_LEDGER`, `AI_ASSISTANT_EXECUTION`, `AI_ASSISTANT_ACTION_JOURNAL` | These schema extensions support accounting/configuration and selected execution/action journals. They do not constitute a general durable conversation transcript. |

## Key relationships and integrity

- `MEAL_PLAN_ENTRY` links a meal-plan entry to its recipe source and stores planned context including servings. Shopping-list creation reads those entries, calculates quantities from the recipe serving basis, aggregates compatible items, and persists list items together with contribution provenance.
- Recipe content uses related ingredient, unit, and media records. Member reactions, saved recipes, views, and comments are represented as related records rather than being embedded in the recipe row.
- Foreign keys, unique constraints, checks, defaults, and indexes are defined through SQL Server schema/migrations as applicable. Entity/service validation complements those database constraints; neither layer alone is a complete specification of all business rules.

## AI persistence boundary

AI tables are schema extensions, not evidence that every chat turn is durably stored. The current Chatbox keeps agent session and pending-confirmation state in memory for a bounded period; the Frontend stores chat history in session storage. V40/V41 add execution/action journal structures for selected workflows. Do not describe these journals as a persistent general-purpose conversation store.

## Diagram and dictionary status

The maintained ERD and Data Dictionary remain unchanged because `docs/diagrams/` is a protected presentation workspace. Their 22-table scope should be presented as the approved core baseline. Before treating them as current comprehensive schema documentation, the project team should manually review and update those artifacts against the latest migration set and snapshot. The SDS should reference the core ERD with this scope note rather than imply it contains all current tables.

## Evidence locations

- `database/schema.sql` — current bootstrap snapshot.
- `app/mamxanh-backend/src/main/resources/db/migration/` — executable Flyway history through V44.
- `app/mamxanh-backend/src/main/java/tech/mamxanh/` — JPA entities and capability services.
- `docs/diagrams/ERD/README.md` and `docs/diagrams/ERD/data-dictionary.md` — protected 22-table core baseline documentation.
