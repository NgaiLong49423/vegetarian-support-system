import { useEffect } from 'react';
import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom';
import { Layout } from './components/Layout';
import { AuthPage } from './pages/Auth';
import { DemoAccountProvider } from './components/DemoAccount';
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

function ScrollToTop() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);
  return null;
}

export default function App() {
  return (
    <BrowserRouter><DemoAccountProvider>
      <ScrollToTop />
      <Layout>
        <Routes>
          <Route path="/dang-nhap" element={<AuthPage key="login" mode="login" />} />
          <Route path="/dang-ky" element={<AuthPage key="register" mode="register" />} />
          <Route path="/quen-mat-khau" element={<AuthPage key="forgot" mode="forgot" />} />
          <Route path="/xac-minh-email" element={<AuthPage key="verify" mode="verify" />} />
          <Route path="/dat-lai-mat-khau" element={<AuthPage key="reset" mode="reset" />} />
          <Route path="/" element={<Home />} />
          <Route path="/kham-pha" element={<Explore />} />
          <Route path="/cong-thuc/:slug" element={<RecipeDetail />} />
          <Route path="/ke-hoach" element={<MealPlanner />} />
          <Route path="/di-cho" element={<ShoppingList />} />
          <Route path="/dinh-duong" element={<NutritionTracker />} />
          <Route path="/dang-cong-thuc" element={<CreateRecipe />} />
          <Route path="/cong-dong" element={<Community />} />
          <Route path="/bai-viet/:slug" element={<PostDetail />} />
          <Route path="/ho-so" element={<Profile />} />
          <Route path="/goi-ai" element={<AiPlans />} />
          <Route path="/giao-dich" element={<TransactionHistory />} />
          <Route path="/ho-so/dinh-duong" element={<NutritionProfile />} />
          <Route path="*" element={<Home />} />
        </Routes>
      </Layout>
    </DemoAccountProvider></BrowserRouter>
  );
}
