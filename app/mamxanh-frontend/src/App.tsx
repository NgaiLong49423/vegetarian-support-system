import { lazy, Suspense, useEffect } from "react"
import { BrowserRouter, Route, Routes, useLocation } from "react-router-dom"
import { AuthProvider } from "./components/AuthContext"
import { LandingPage } from "./pages/LandingPage"

const ApplicationRoutes = lazy(() => import("./components/ApplicationRoutes"))

function ScrollToTop() {
  const { pathname } = useLocation()
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])
  return null
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ScrollToTop />
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route
            path="*"
            element={
              <Suspense
                fallback={
                  <main role="status" className="p-8">
                    Đang mở ứng dụng…
                  </main>
                }
              >
                <ApplicationRoutes />
              </Suspense>
            }
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
