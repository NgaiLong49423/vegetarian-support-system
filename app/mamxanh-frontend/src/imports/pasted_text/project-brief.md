Build a complete frontend web application based on all of the attached reference screenshots.

PROJECT
Name: Mâm Xanh

Mâm Xanh is a vegetarian support web application that helps users discover vegetarian recipes, read community food posts, plan weekly meals, and manage ingredients/shopping lists.

IMPORTANT:
- Treat the attached screenshots as the primary visual reference.
- Analyze ALL attached screenshots before building.
- Preserve the overall layout, visual hierarchy, spacing, card styles, navigation patterns, and interaction patterns from the references.
- Do not simply recreate each screenshot as an isolated page.
- Convert them into one consistent, reusable application design system.
- You may improve unclear or missing UX details, but keep the result visually consistent with the references.
- Do not add unrelated features.

TECH STACK
Use:
- React
- TypeScript
- Modern functional components
- Reusable components
- Clean component architecture

This is FRONTEND ONLY for now.

Do NOT implement a real backend, database, authentication server, payment system, or external API.

Use realistic mock data where data is required.

Structure the code so that mock data can later be replaced by REST API calls from a Spring Boot backend.

LANGUAGE
All user-facing content must be in Vietnamese.

APP STYLE
The application should feel:
- clean
- modern
- friendly
- food-oriented
- vegetarian-oriented
- easy to understand
- suitable for a university software project but polished enough to look like a real product

Use a natural green-centered visual identity suitable for the Mâm Xanh brand.

Avoid:
- excessive gradients
- overly decorative UI
- giant text
- unnecessary animations
- excessive rounded cards
- inconsistent spacing
- random colors

DESIGN SYSTEM
Create a consistent design system for:
- colors
- typography
- spacing
- border radius
- buttons
- input fields
- cards
- badges
- tabs
- navigation
- dialogs
- empty states
- loading states

Use the same component whenever the same UI pattern appears across multiple screens.

GLOBAL LAYOUT
Create a consistent application shell containing:
- top navigation/header
- Mâm Xanh logo/brand area
- navigation links
- search where appropriate
- notification/profile area where appropriate
- main page container
- responsive behavior

Navigation must work between pages.

MAIN APPLICATION AREAS

1. TRANG CHỦ
Create a homepage inspired by the reference screenshots.

Include appropriate sections such as:
- greeting or introductory section
- featured vegetarian recipes
- recommended recipes
- popular content/posts
- useful shortcuts
- recipe categories

Recipe cards should support information such as:
- image
- recipe name
- preparation time
- difficulty where appropriate
- vegetarian category/tag
- rating or engagement information where appropriate
- save/favorite action

Do not make every section visually identical.

2. KHÁM PHÁ CÔNG THỨC
Create a recipe discovery/browse page.

Include:
- search
- filters
- categories
- sorting where useful
- recipe grid/list
- vegetarian diet tags
- pagination or load-more behavior if appropriate

Example filters:
- loại món
- thời gian nấu
- mức độ khó
- chế độ ăn chay
- nguyên liệu

3. CHI TIẾT CÔNG THỨC
Create a detailed recipe page.

Include:
- recipe image
- recipe title
- description
- preparation/cooking time
- servings
- difficulty
- vegetarian category
- ingredients
- cooking instructions
- author information where relevant
- save recipe
- add recipe to meal plan
- add ingredients to shopping list

The layout should clearly separate recipe information, ingredients, and cooking steps.

4. KẾ HOẠCH BỮA ĂN
Create a weekly Meal Planner.

The user should be able to see meals organized by:
- day of week
- meal slot where appropriate

Support UI interactions such as:
- add recipe
- replace recipe
- remove recipe
- view recipe
- navigate between weeks

Keep the planner visually understandable and not overly complex.

5. DANH SÁCH MUA SẮM
Create a Shopping List / Grocery Checklist generated from recipes selected by the user.

Include:
- grouped ingredients
- ingredient name
- required quantity
- checkbox for purchased/already available items
- ability to remove an item
- clear completed items
- export shopping list action

Design the page so that it could later receive automatically aggregated ingredient data from the Meal Planner.

6. CỘNG ĐỒNG / BÀI VIẾT
Create a community content page based on the attached references.

Include:
- post feed
- food/recipe related posts
- author
- image
- title
- short content preview
- tags
- engagement information such as likes/comments where appropriate

Include sections for popular or trending posts if they fit the reference design.

Do NOT implement a complex social network.

7. CHI TIẾT BÀI VIẾT
Create a readable post detail page with:
- title
- author
- publication information
- main image
- formatted article body
- tags
- engagement/actions where appropriate
- related posts

8. HỒ SƠ NGƯỜI DÙNG
Create a user profile/settings area.

Possible sections:
- thông tin cá nhân
- sở thích ăn chay
- món ăn yêu thích
- công thức đã lưu
- tùy chọn cơ bản

Keep it simple.

INTERACTIONS
Make the prototype/application functional enough for demonstration.

At minimum:
- navigation links work
- buttons provide visible feedback
- tabs can switch content
- filters can change displayed mock data
- recipe save/favorite can toggle
- shopping list checkboxes can toggle
- dialogs/dropdowns work where used
- Meal Planner interactions should have basic frontend behavior

Use local state for interactions.

MOCK DATA
Create realistic Vietnamese vegetarian mock data.

Example dishes may include:
- Phở chay
- Bún Huế chay
- Cơm gạo lứt rau củ
- Đậu hũ sốt nấm
- Gỏi cuốn chay
- Canh nấm
- Mì xào rau củ

Use realistic ingredient names and Vietnamese UI text.

Do not use lorem ipsum.

RESPONSIVE DESIGN
Prioritize desktop web layout because the attached references are desktop-oriented.

Also make the interface reasonably responsive for:
- desktop
- tablet
- mobile

Do not sacrifice the desktop design just to make everything look like a mobile app.

CODE QUALITY
Use reusable components such as:
- AppHeader
- Navigation
- SearchBar
- RecipeCard
- RecipeGrid
- PostCard
- FilterPanel
- MealPlanCard
- IngredientItem
- ShoppingList
- UserMenu
- Modal
- EmptyState

Avoid:
- putting the entire application into one component
- duplicating large UI blocks
- hard-coded repeated markup
- creating separate unrelated design styles for every page

Use TypeScript types/interfaces for major domain objects such as:
- Recipe
- Ingredient
- MealPlan
- MealPlanItem
- ShoppingListItem
- Post
- User

UX DETAILS
Include appropriate:
- hover states
- active navigation state
- selected states
- disabled states
- empty states
- confirmation dialogs for destructive actions where useful

Make clickable elements visually recognizable.

FINAL GOAL
The result should look like a coherent real product called “Mâm Xanh”, not a collection of disconnected generated screens.

First analyze the attached screenshots and establish the shared design language.

Then build the complete React + TypeScript frontend application using that design language.

When something is not visible in the screenshots, extend the design using the SAME visual language rather than inventing a completely different style.