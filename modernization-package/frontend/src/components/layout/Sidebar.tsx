import { NavLink } from "react-router-dom"
import { LayoutDashboard, List, Eye, PlusCircle, CreditCard } from "lucide-react"
import { cn } from "@/lib/utils"
import { Separator } from "@/components/ui/separator"

const navItems = [
  { to: "/", icon: LayoutDashboard, label: "Dashboard" },
  { to: "/transactions", icon: List, label: "Transaction List" },
  { to: "/transactions/view", icon: Eye, label: "View Transaction" },
  { to: "/transactions/add", icon: PlusCircle, label: "Add Transaction" },
]

export default function Sidebar() {
  return (
    <aside className="hidden md:flex md:w-64 md:flex-col md:fixed md:inset-y-0 border-r bg-white">
      {/* Logo area */}
      <div className="flex items-center gap-2 px-6 py-5">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary">
          <CreditCard className="h-5 w-5 text-white" />
        </div>
        <div>
          <h1 className="text-lg font-semibold text-foreground">CardDemo</h1>
          <p className="text-xs text-muted-foreground">Transaction Processing</p>
        </div>
      </div>

      <Separator />

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        <p className="px-3 mb-2 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
          Module
        </p>
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === "/"}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all",
                isActive
                  ? "bg-primary/10 text-primary"
                  : "text-muted-foreground hover:bg-muted hover:text-foreground"
              )
            }
          >
            <item.icon className="h-4 w-4" />
            {item.label}
          </NavLink>
        ))}
      </nav>

      {/* Footer */}
      <div className="border-t px-6 py-4">
        <p className="text-xs text-muted-foreground">
          Modernized from COBOL/CICS
        </p>
        <p className="text-xs text-muted-foreground">
          Java 21 &middot; Spring Boot 3 &middot; React
        </p>
      </div>
    </aside>
  )
}
