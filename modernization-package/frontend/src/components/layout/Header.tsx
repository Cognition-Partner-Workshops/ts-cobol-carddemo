import { useLocation } from "react-router-dom"
import { CreditCard, Menu } from "lucide-react"
import { Button } from "@/components/ui/button"

const pageTitles: Record<string, string> = {
  "/": "Dashboard",
  "/transactions": "Transaction List",
  "/transactions/view": "View Transaction",
  "/transactions/add": "Add Transaction",
}

interface HeaderProps {
  onMobileMenuToggle?: () => void
}

export default function Header({ onMobileMenuToggle }: HeaderProps) {
  const location = useLocation()

  const getTitle = () => {
    if (location.pathname.startsWith("/transactions/view/")) return "View Transaction"
    return pageTitles[location.pathname] || "CardDemo"
  }

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-4 border-b bg-white px-6">
      {/* Mobile menu button */}
      <Button
        variant="ghost"
        size="icon"
        className="md:hidden"
        onClick={onMobileMenuToggle}
      >
        <Menu className="h-5 w-5" />
      </Button>

      {/* Mobile logo */}
      <div className="flex items-center gap-2 md:hidden">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
          <CreditCard className="h-4 w-4 text-white" />
        </div>
        <span className="font-semibold">CardDemo</span>
      </div>

      {/* Page title */}
      <div className="flex-1">
        <h2 className="text-lg font-semibold">{getTitle()}</h2>
      </div>

      {/* Right side */}
      <div className="flex items-center gap-2">
        <span className="hidden sm:inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800">
          Connected
        </span>
      </div>
    </header>
  )
}
