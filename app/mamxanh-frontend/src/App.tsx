import { lazy, Suspense, useEffect } from "react"

import { BrowserRouter, Route, Routes, useLocation } from "react-router-dom"

import { AuthProvider } from "./components/AuthContext"

import { ThemeProvider } from "./components/ThemeContext"

import { LandingPage } from "./pages/LandingPage"

import { AiChatHost } from "./components/AiChatHost"
import { HamsterLoader } from "./components/HamsterLoader"

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
    <ThemeProvider>
      <BrowserRouter>
        <AuthProvider>
          <AiChatHost>
            <ScrollToTop />
            <Routes>
              <Route
                path="/"
                element={
                  <div key="landing" className="page-transition w-full">
                    <LandingPage />
                  </div>
                }
              />
              <Route
                path="*"
                element={
                  <Suspense
                    fallback={
                      <main
                        role="status"
                        className="flex min-h-[60vh] flex-col items-center justify-center p-8"
                      >
                        <HamsterLoader
                          size="lg"
                          text="Đang mở ứng dụng Mâm Xanh…"
                        />
                      </main>
                    }
                  >
                    <ApplicationRoutes />
                  </Suspense>
                }
              />
            </Routes>
          </AiChatHost>
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  )
}
