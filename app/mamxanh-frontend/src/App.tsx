import { useEffect } from 'react';
import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom';
import { Layout } from './components/Layout';
import { AuthPage } from './pages/Auth';
import { AuthProvider } from './components/AuthContext';
import { Home } from './pages/Home';
import { Explore } from './pages/Explore';
import { RecipeDetail } from './pages/RecipeDetail';
import { MealPlanner } from './pages/MealPlanner';
import { ShoppingList } from './pages/ShoppingList';
import { NutritionTracker } from './pages/NutritionTracker';
import { CreateRecipe } from './pages/CreateRecipe';
import { Community } from './pages/Community';
import { PostDetail } from './pages/PostDetail';
import { Profile } from './pages/Profile';
import { AiPlans } from './pages/AiPlans';
import { TransactionHistory } from './pages/TransactionHistory';
import { NutritionProfile } from './pages/NutritionProfile';
import { DietaryPreferencesPage } from './pages/DietaryPreferencesPage';
import { OnboardingPage } from './pages/OnboardingPage';
import { AdminCatalogPage } from './pages/AdminCatalogPage';
import { ExpertApplicationPage } from './pages/ExpertApplication';
import { RecipeComparePage } from './pages/RecipeCompare';
import { EditRecipe } from './pages/EditRecipe';
import { ApiRecipeDetail } from './pages/ApiRecipeDetail';
import { ExpertRecipeProfile } from './pages/ExpertRecipeProfile';
import { ClickSpark } from './components/ClickSpark';

function ScrollToTop() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);
  return null;
}

export default function App() {
  return (
    <BrowserRouter><AuthProvider>
      <ScrollToTop />
      <ClickSpark sparkColor="#ea580c" sparkSize={11} sparkRadius={20} sparkCount={8} duration={420}>
        <Layout>
        <Routes>
          <Route path="/dang-nhap" element={<AuthPage key="login" mode="login" />} />
          <Route path="/dang-ky" element={<AuthPage key="register" mode="register" />} />
          <Route path="/quen-mat-khau" element={<AuthPage key="forgot" mode="forgot" />} />
          <Route path="/xac-minh-email" element={<AuthPage key="verify" mode="verify" />} />
          <Route path="/dat-lai-mat-khau" element={<AuthPage key="reset" mode="reset" />} />
          <Route path="/" element={<Home />} />
          <Route path="/kham-pha" element={<Explore />} />
          <Route path="/cong-thuc/:recipeId/chinh-sua" element={<EditRecipe />} />
          <Route path="/cong-thuc/id/:recipeId" element={<ApiRecipeDetail />} />
          <Route path="/cong-thuc/:slug" element={<RecipeDetail />} />
          <Route path="/ke-hoach" element={<MealPlanner />} />
          <Route path="/di-cho" element={<ShoppingList />} />
          <Route path="/dinh-duong" element={<NutritionTracker />} />
          <Route path="/dang-cong-thuc" element={<CreateRecipe />} />
          <Route path="/cong-dong" element={<Community />} />
          <Route path="/bai-viet/:slug" element={<PostDetail />} />
          <Route path="/ho-so" element={<Profile />} />
          <Route path="/ho-so/chuyen-gia" element={<ExpertRecipeProfile />} />
          <Route path="/goi-ai" element={<AiPlans />} />
          <Route path="/giao-dich" element={<TransactionHistory />} />
          <Route path="/ho-so/dinh-duong" element={<NutritionProfile />} />
          <Route path="/ho-so/so-thich-an-uong" element={<DietaryPreferencesPage />} />
          <Route path="/khoi-tao-so-thich" element={<OnboardingPage />} />
          <Route path="/quan-tri/danh-muc" element={<AdminCatalogPage />} />
          <Route path="/dang-ky-chuyen-gia" element={<ExpertApplicationPage />} />
          <Route path="/admin/xet-duyet-chuyen-gia" element={<ExpertApplicationPage />} />
          <Route path="/so-sanh" element={<RecipeComparePage />} />
          <Route path="*" element={<Home />} />
        </Routes>
        </Layout>
      </ClickSpark>
    </AuthProvider></BrowserRouter>
  );
}
