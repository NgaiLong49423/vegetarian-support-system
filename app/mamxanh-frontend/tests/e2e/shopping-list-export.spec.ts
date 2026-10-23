import { expect, test } from "./baseFixtures";
import { seedDemoSession } from "./demo-session";
import type { Page } from "@playwright/test";
import type { ShoppingItem } from "../../src/api/shopping";

const item = (id: number, extra: Partial<ShoppingItem>): ShoppingItem => ({
  id,
  ingredientId: id,
  name: "Đậu hũ",
  ingredientGroup: "Đậu hạt",
  quantity: 600,
  unitId: 1,
  unitCode: "g",
  unitName: "gram",
  bought: false,
  ...extra,
});
const initial = [
  item(1, {
    sources: [
      {
        recipeId: 999,
        title: "PRIVATE_SOURCE_METADATA",
        available: true,
        originalQuantity: 300,
        originalUnitId: 1,
        originalUnitCode: "g",
      },
    ],
  }),
  item(2, {
    name: "Nấm rơm",
    ingredientGroup: "Nấm",
    quantity: 125.5,
    bought: true,
  }),
  item(3, {
    name: "Nước tương",
    ingredientGroup: "Gia vị",
    quantity: 200,
    unitId: 2,
    unitCode: "ml",
  }),
  item(4, {
    name: "Khăn giấy lau bếp",
    ingredientId: null,
    ingredientGroup: "Nhóm khác",
    quantity: 2,
    unitId: 3,
    unitCode: "cuộn",
    bought: true,
  }),
  item(5, { name: "Cải ngọt", ingredientGroup: "Rau củ", quantity: 1400 }),
];
async function setup(
  page: Page,
  mode: "normal" | "empty" | "foreign" = "normal",
  listName = "Đi chợ đã lưu",
) {
  await seedDemoSession(page, "CUSTOMER");
  const items = mode === "empty" ? [] : structuredClone(initial);
  const writes: string[] = [];
  await page.route("**/api/v1/shopping-lists**", async (route) => {
    const req = route.request(),
      url = new URL(req.url()),
      suffix = url.pathname.replace("/api/v1/shopping-lists", "");
    if (req.method() !== "GET") {
      writes.push(req.method());
      const row = items.find((i) => i.id === Number(suffix.split("/")[3]))!;
      row.bought = req.postDataJSON().bought;
      return route.fulfill({ json: row });
    }
    if (suffix === "/options")
      return route.fulfill({
        json: [{ id: 1, code: "g", name: "gram", active: true }],
      });
    const summary = {
      id: 42,
      name: listName,
      createdAt: "",
      updatedAt: "",
    };
    if (!suffix)
      return route.fulfill({
        json: {
          content: [summary, { ...summary, id: 43, name: "Danh sách thứ hai" }],
          page: 0,
          size: 20,
          totalElements: 2,
          totalPages: 1,
        },
      });
    if (mode === "foreign")
      return route.fulfill({
        status: 404,
        contentType: "application/problem+json",
        json: {
          status: 404,
          code: "NOT_FOUND",
          detail: "Không tìm thấy danh sách.",
        },
      });
    return route.fulfill({
      json: {
        ...summary,
        id: Number(suffix.slice(1)),
        items:
          suffix === "/43"
            ? [item(9, { name: "Danh sách khác", quantity: 10 })]
            : items,
      },
    });
  });
  await page.goto("/di-cho?listId=42");
  if (mode !== "foreign")
    await expect(
      page.getByRole("heading", { name: listName, exact: true }),
    ).toBeVisible();
  return { items, writes };
}
async function clipboard(page: Page) {
  return (await page.evaluate(() => navigator.clipboard.readText())).replaceAll(
    "\r\n",
    "\n",
  );
}

for (const [name, expected] of [
  ["Đi chợ · thực đơn 23/10/2026", "Ngày thực đơn: 23/10/2026"],
  ["Đi chợ · thực đơn tuần 19–25/10/2026", "Ngày thực đơn: 19/10/2026–25/10/2026"],
  ["Đi chợ · thực đơn 20, 23, 25/10/2026", "Ngày thực đơn: 20/10/2026, 23/10/2026, 25/10/2026"],
  ["Đi chợ · thực đơn 28/10–03/11/2026", "Ngày thực đơn: 28/10/2026–03/11/2026"],
  ["Đi chợ · thực đơn 28/12/2026–03/01/2027", "Ngày thực đơn: 28/12/2026–03/01/2027"],
  ["Đi chợ · thực đơn 28/12/2026, 01/01/2027", "Ngày thực đơn: 28/12/2026, 01/01/2027"],
  ["Danh sách cá nhân từ công thức", "Nguồn: Công thức đã chọn — không gắn ngày thực đơn"],
  ["Đi chợ tuần 2026-10-19", "Ngày thực đơn: Chưa có thông tin ngày trong danh sách này."],
]) {
  test(`TXT and clipboard keep export date separate from saved meal dates: ${name}`, async ({ page, context }) => {
    await context.grantPermissions(["clipboard-read", "clipboard-write"]);
    await page.clock.setFixedTime(new Date(2026, 9, 20, 12));
    await setup(page, "normal", name);
    await page.getByRole("button", { name: "Sao chép", exact: true }).click();
    await expect(page.getByRole("status")).toHaveText("Đã sao chép danh sách vào khay nhớ tạm.");
    const copied = await clipboard(page);
    expect(copied).toContain(`Ngày xuất: 20/10/2026\n${expected}\n`);
    const event = page.waitForEvent("download");
    await page.getByRole("button", { name: "TXT", exact: true }).click();
    const download = await event;
    expect(download.suggestedFilename()).toBe("Danh_sach_mua_sam_20261020.txt");
    const stream = await download.createReadStream(), chunks: Buffer[] = [];
    for await (const chunk of stream) chunks.push(Buffer.from(chunk));
    expect(Buffer.concat(chunks).subarray(3).toString("utf8")).toBe(copied);
  });
}
test("AC-55.1/3: exports every saved item across filters with groups, exact units and no provenance", async ({
  page,
  context,
}) => {
  await context.grantPermissions(["clipboard-read", "clipboard-write"]);
  const state = await setup(page);
  await page.getByRole("button", { name: /^Cần mua/ }).click();
  await page.getByRole("button", { name: "Sao chép", exact: true }).click();
  await expect(page.getByRole("status")).toHaveText(
    "Đã sao chép danh sách vào khay nhớ tạm.",
  );
  const content = await clipboard(page);
  expect(content).toContain("[Đậu hạt]\n[ ] Đậu hũ: 600 g");
  expect(content).toContain("[Nấm]\n[x] Nấm rơm: 125,5 g");
  expect(content).toContain("[x] Khăn giấy lau bếp: 2 cuộn");
  expect(content).toContain("[ ] Cải ngọt: 1.400 g");
  expect(content).toContain("[ ] Nước tương: 200 ml");
  expect(content).not.toContain("PRIVATE_SOURCE_METADATA");
  expect(content).not.toContain("999");
  expect(state.writes).toEqual([]);
  expect(state.items).toEqual(initial);
  await page.screenshot({
    path: test.info().outputPath("detail-shopping-export-desktop.png"),
    fullPage: true,
  });
});
test("AC-55.2/3: UTF-8 BOM download matches clipboard, filename and saved bought state after reload", async ({
  page,
  context,
}) => {
  await context.grantPermissions(["clipboard-read", "clipboard-write"]);
  const state = await setup(page);
  await page
    .getByRole("button", { name: "Đánh dấu Đậu hũ g", exact: true })
    .click();
  await expect(
    page.getByRole("button", { name: "Đánh dấu Đậu hũ g", exact: true }),
  ).toHaveAttribute("aria-pressed", "true");
  await page.reload();
  await expect(
    page.getByRole("button", { name: "Đánh dấu Đậu hũ g", exact: true }),
  ).toHaveAttribute("aria-pressed", "true");
  await page.getByRole("button", { name: "Sao chép", exact: true }).click();
  await expect(page.getByRole("status")).toHaveText(
    "Đã sao chép danh sách vào khay nhớ tạm.",
  );
  const content = await clipboard(page);
  const downloaded = page.waitForEvent("download");
  await page.getByRole("button", { name: "TXT", exact: true }).click();
  const file = await downloaded;
  expect(file.suggestedFilename()).toMatch(/^Danh_sach_mua_sam_\d{8}\.txt$/);
  const stream = await file.createReadStream(),
    chunks: Buffer[] = [];
  for await (const chunk of stream) chunks.push(Buffer.from(chunk));
  const bytes = Buffer.concat(chunks);
  expect([...bytes.subarray(0, 3)]).toEqual([0xef, 0xbb, 0xbf]);
  expect(bytes.subarray(3).toString("utf8")).toBe(content);
  expect(content).toContain("[x] Đậu hũ: 600 g");
  expect(state.writes).toEqual(["PATCH"]);
});
test("AC-55.4: empty saved checklist disables both exports", async ({
  page,
}) => {
  await setup(page, "empty");
  await expect(
    page.getByRole("button", { name: "Sao chép", exact: true }),
  ).toBeDisabled();
  await expect(
    page.getByRole("button", { name: "TXT", exact: true }),
  ).toBeDisabled();
});
for (const failure of ["missing", "denied"] as const) {
  test(`EF-55.2: ${failure} clipboard opens selectable fallback with keyboard focus on mobile`, async ({
    page,
  }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await page.addInitScript(
      (mode) =>
        Object.defineProperty(navigator, "clipboard", {
          configurable: true,
          value:
            mode === "missing"
              ? undefined
              : {
                  writeText: async () => {
                    throw new DOMException("Denied", "NotAllowedError");
                  },
                },
        }),
      failure,
    );
    const state = await setup(page);
    await page.getByRole("button", { name: "Sao chép", exact: true }).click();
    const dialog = page.getByRole("dialog", {
      name: "Sao chép danh sách đi chợ",
    });
    const textarea = dialog.getByRole("textbox", {
      name: "Nội dung danh sách đi chợ",
    });
    await expect(textarea).toBeFocused();
    expect(await textarea.inputValue()).toContain("[x] Nấm rơm: 125,5 g");
    await dialog
      .getByRole("button", { name: "Chọn tất cả", exact: true })
      .click();
    expect(
      await textarea.evaluate(
        (el) =>
          (el as HTMLTextAreaElement).selectionEnd -
          (el as HTMLTextAreaElement).selectionStart,
      ),
    ).toBe((await textarea.inputValue()).length);
    await page.screenshot({
      path: test.info().outputPath("detail-shopping-export-fallback-mobile.png"),
      fullPage: true,
    });
    await page.keyboard.press("Escape");
    await expect(dialog).not.toBeVisible();
    await expect(
      page.getByRole("button", { name: "Sao chép", exact: true }),
    ).toBeFocused();
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    expect(state.writes).toEqual([]);
  });
}
test("export uses only the currently selected list and is unavailable on foreign list or Guest", async ({
  page,
  context,
}) => {
  await context.grantPermissions(["clipboard-read", "clipboard-write"]);
  await setup(page);
  await page
    .getByRole("navigation", { name: "Danh sách đã lưu" })
    .getByRole("button", { name: "Danh sách thứ hai" })
    .click();
  await expect(page.getByText("Danh sách khác", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Sao chép", exact: true }).click();
  await expect(page.getByRole("status")).toHaveText(
    "Đã sao chép danh sách vào khay nhớ tạm.",
  );
  expect(await clipboard(page)).toContain("[ ] Danh sách khác: 10 g");
  expect(await clipboard(page)).not.toContain("Nấm rơm");
  await page.unroute("**/api/v1/shopping-lists**");
  await setup(page, "foreign");
  await expect(
    page.getByText("Không tìm thấy danh sách.", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "TXT", exact: true }),
  ).toHaveCount(0);
  const guest = await context.newPage();
  await guest.goto("/di-cho");
  await expect(
    guest.getByText(
      "Vui lòng đăng nhập để tạo và lưu danh sách đi chợ theo tài khoản.",
    ),
  ).toBeVisible();
  await expect(
    guest.getByRole("button", { name: "Sao chép", exact: true }),
  ).toHaveCount(0);
  await guest.close();
});
test("download preparation failure reports retry without changing the checklist", async ({
  page,
}) => {
  const state = await setup(page);
  await page.evaluate(() => {
    URL.createObjectURL = () => {
      throw new Error("No blob URL available");
    };
  });
  await page.getByRole("button", { name: "TXT", exact: true }).click();
  await expect(page.getByRole("status")).toHaveText(
    "Chưa thể tạo tệp TXT. Vui lòng thử lại hoặc sao chép danh sách.",
  );
  expect(state.items).toEqual(initial);
  expect(state.writes).toEqual([]);
});
