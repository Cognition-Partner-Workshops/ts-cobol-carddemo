import { NavLink } from "react-router-dom"
import { LayoutDashboard, List, Eye, PlusCircle, CreditCard, X } from "lucide-react"
import { cn } from "@/lib/utils"
import { Separator } from "@/components/ui/separator"

const navItems = [
  { to: "/", icon: LayoutDashboard, label: "Dashboard" },
  { to: "/transactions", icon: List, label: "Transaction List" },
  { to: "/transactions/view", icon: Eye, label: "View Transaction" },
  { to: "/transactions/add", icon: PlusCircle, label: "Add Transaction" },
]

interface MobileSidebarProps {
  open: boolean
  onClose: () => void
}

export default function MobileSidebar({ open, onClose }: MobileSidebarProps) {
  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 md:hidden">
      <div className="fixed inset-0 bg-black/50" onClick={onClose} />
      <div className="fixed inset-y-0 left-0 w-64 bg-white shadow-lg">
        <div className="flex items-center justify-between px-6 py-5">
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary">
              <CreditCard className="h-5 w-5 text-white" />
            </div>
            <h1 className="text-lg font-semibold">CardDemo</h1>
          </div>
          <button onClick={onClose}>
            <X className="h-5 w-5 text-muted-foreground" />
          </button>
        </div>

        <Separator />

        <nav className="px-3 py-4 space-y-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/"}
              onClick={onClose}
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
      </div>
    </div>
  )
}
