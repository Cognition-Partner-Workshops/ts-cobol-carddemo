import { useState } from "react"
import { Outlet } from "react-router-dom"
import Sidebar from "./Sidebar"
import Header from "./Header"
import MobileSidebar from "./MobileSidebar"

export default function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false)

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Desktop sidebar */}
      <Sidebar />

      {/* Mobile sidebar */}
      <MobileSidebar open={mobileOpen} onClose={() => setMobileOpen(false)} />

      {/* Main content */}
      <div className="md:pl-64">
        <Header onMobileMenuToggle={() => setMobileOpen(true)} />
        <main className="p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
