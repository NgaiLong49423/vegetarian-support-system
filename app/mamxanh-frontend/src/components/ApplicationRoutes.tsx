import { Navigate, Route, Routes } from "react-router-dom"
import { Layout } from "./Layout"
import { AuthPage } from "../pages/Auth"
import { SavedRecipesProvider } from "./SavedRecipesContext"
import { Home } from "../pages/Home"
import { Explore } from "../pages/Explore"
import { RecipeDetail } from "../pages/RecipeDetail"
import { MealPlanner } from "../pages/MealPlanner"
import { ShoppingList } from "../pages/ShoppingList"
import { NutritionTracker } from "../pages/NutritionTracker"
import { CreateRecipe } from "../pages/CreateRecipe"
import { Community } from "../pages/Community"
import { PostDetail } from "../pages/PostDetail"
import { Profile } from "../pages/Profile"
import { AiPlans } from "../pages/AiPlans"
import { TransactionHistory } from "../pages/TransactionHistory"
import { PaymentResult } from "../pages/PaymentResult"
import { NutritionProfile } from "../pages/NutritionProfile"
import { DietaryPreferencesPage } from "../pages/DietaryPreferencesPage"
import { OnboardingPage } from "../pages/OnboardingPage"
import { AdminCatalogPage } from "../pages/AdminCatalogPage"
import { ExpertApplicationPage } from "../pages/ExpertApplication"
import { RecipeComparePage } from "../pages/RecipeCompare"
import { RecipeComparePickerPage } from "../pages/RecipeComparePicker"
import { EditRecipe } from "../pages/EditRecipe"
import { ApiRecipeDetail } from "../pages/ApiRecipeDetail"
import { ExpertRecipeProfile } from "../pages/ExpertRecipeProfile"
import { MemberProfilePage } from "../pages/MemberProfilePage"
import { ProfileSettingsPage } from "../pages/ProfileSettingsPage"
import { NotificationsPage } from "../pages/NotificationsPage"
import { AiChatPage } from "../pages/AiChatPage"
import { AdminDashboard } from "../pages/AdminDashboard"
import { AdminPendingModule } from "../pages/AdminPendingModule"
import { ExpertDirectoryPage } from "../pages/ExpertDirectoryPage"
import { IngredientDirectoryPage } from "../pages/IngredientDirectoryPage"
import { IngredientDetailPage } from "../pages/IngredientDetailPage"
import { ClickSpark } from "./ClickSpark"

export default function ApplicationRoutes() {
  return (
    <SavedRecipesProvider>
        <ClickSpark
          sparkColor="#ea580c"
          sparkSize={11}
          sparkRadius={20}
          sparkCount={8}
          duration={420}
        >
          <Layout>
            <Routes>
              <Route
                path="/dang-nhap"
                element={<AuthPage key="login" mode="login" />}
              />
              <Route
                path="/dang-ky"
                element={<AuthPage key="register" mode="register" />}
              />
              <Route
                path="/quen-mat-khau"
                element={<AuthPage key="forgot" mode="forgot" />}
              />
              <Route
                path="/xac-minh-email"
                element={<AuthPage key="verify" mode="verify" />}
              />
              <Route
                path="/dat-lai-mat-khau"
                element={<AuthPage key="reset" mode="reset" />}
              />
              <Route path="/trang-chu" element={<Home />} />
              <Route path="/kham-pha" element={<Explore />} />
              <Route path="/chuyen-gia" element={<ExpertDirectoryPage />} />
              <Route path="/danh-muc-nguyen-lieu" element={<IngredientDirectoryPage />} />
              <Route path="/nguyen-lieu/:ingredientId" element={<IngredientDetailPage />} />
              <Route
                path="/cong-thuc/:recipeId/chinh-sua"
                element={<EditRecipe />}
              />
              <Route
                path="/cong-thuc/id/:recipeId"
                element={<ApiRecipeDetail />}
              />
              <Route path="/cong-thuc/:slug" element={<RecipeDetail />} />
              <Route path="/ke-hoach" element={<MealPlanner />} />
              <Route path="/goi-y-ai" element={<Navigate to="/ke-hoach?view=for-you" replace />} />
              <Route path="/hoi-dap-ai" element={<AiChatPage />} />
              <Route path="/di-cho" element={<ShoppingList />} />
              <Route path="/dinh-duong" element={<NutritionTracker />} />
              <Route path="/dang-cong-thuc" element={<CreateRecipe />} />
              <Route path="/cong-dong" element={<Community />} />
              <Route path="/bai-viet/:slug" element={<PostDetail />} />
              <Route path="/ho-so" element={<Profile />} />
              <Route path="/ho-so/cai-dat" element={<ProfileSettingsPage />} />
              <Route path="/thong-bao" element={<NotificationsPage />} />
              <Route path="/thanh-vien/:userId" element={<MemberProfilePage />} />
              <Route
                path="/ho-so/chuyen-gia"
                element={<ExpertRecipeProfile />}
              />
              <Route path="/goi-ai" element={<AiPlans />} />
              <Route path="/payment/success" element={<PaymentResult mode="success" />} />
              <Route path="/payment/cancel" element={<PaymentResult mode="cancel" />} />
              <Route path="/giao-dich" element={<TransactionHistory />} />
              <Route path="/ho-so/dinh-duong" element={<NutritionProfile />} />
              <Route
                path="/ho-so/so-thich-an-uong"
                element={<DietaryPreferencesPage />}
              />
              <Route path="/khoi-tao-so-thich" element={<OnboardingPage />} />
              <Route path="/quan-tri/danh-muc" element={<AdminCatalogPage />} />
              <Route
                path="/dang-ky-chuyen-gia"
                element={<ExpertApplicationPage />}
              />
              <Route
                path="/admin/xet-duyet-chuyen-gia"
                element={<ExpertApplicationPage />}
              />
              <Route path="/admin" element={<AdminDashboard />} />
              <Route path="/admin/tong-quan" element={<AdminDashboard />} />
              <Route
                path="/admin/bao-cao"
                element={<AdminPendingModule module="reports" />}
              />
              <Route
                path="/admin/nguoi-dung"
                element={<AdminPendingModule module="users" />}
              />
              <Route
                path="/admin/giao-dich"
                element={<AdminPendingModule module="transactions" />}
              />
              <Route path="/so-sanh" element={<RecipeComparePage />} />
              <Route path="/so-sanh/chon-mon" element={<RecipeComparePickerPage />} />
              <Route path="*" element={<Home />} />
            </Routes>
          </Layout>
        </ClickSpark>
      </SavedRecipesProvider>
  )
}
