import { useEffect } from 'react';
import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom';
import { Layout } from './components/Layout';
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

function ScrollToTop() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);
  return null;
}

export default function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <Layout>
        <Routes>
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
          <Route path="*" element={<Home />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  );
}
