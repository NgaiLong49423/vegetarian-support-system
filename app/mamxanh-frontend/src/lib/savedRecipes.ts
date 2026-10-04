/* ============================================================
   MODULE: LƯU BÀI (BOOKMARK) — dùng localStorage
   ============================================================ */

const KEY = "mamxanh_saved_recipes";

export interface SavedRecipe {
    slug: string;
    name: string;
    image: string;
    diet: string;
    calories: number;
    prepTime: number;
    cookTime: number;
    author: string;
    savedAt: number;
}

function getAll(): SavedRecipe[] {
    try {
        return JSON.parse(localStorage.getItem(KEY) || "[]");
    } catch {
        return [];
    }
}

function saveAll(list: SavedRecipe[]): void {
    localStorage.setItem(KEY, JSON.stringify(list));
    window.dispatchEvent(new CustomEvent("mamxanh:saved-changed", { detail: list }));
}

export function isSaved(slug: string): boolean {
    return getAll().some((x) => x.slug === slug);
}

export function getSavedRecipeSlugs(): string[] {
    return getAll().map((recipe) => recipe.slug);
}

export function toggleSaved(recipe: Omit<SavedRecipe, "savedAt">): boolean {
    const list = getAll();
    const idx = list.findIndex((x) => x.slug === recipe.slug);
    if (idx >= 0) {
        list.splice(idx, 1);
    } else {
        list.push({ ...recipe, savedAt: Date.now() });
    }
    saveAll(list);
    return isSaved(recipe.slug);
}

export function removeSaved(slug: string): void {
    const list = getAll().filter((x) => x.slug !== slug);
    saveAll(list);
}

export function countSaved(): number {
    return getAll().length;
}

export function subscribeSaved(cb: (list: SavedRecipe[]) => void): () => void {
    const handler = (e: Event) => {
        const detail = (e as CustomEvent).detail as SavedRecipe[];
        cb(detail || getAll());
    };
    window.addEventListener("mamxanh:saved-changed", handler);
    return () => window.removeEventListener("mamxanh:saved-changed", handler);
}
