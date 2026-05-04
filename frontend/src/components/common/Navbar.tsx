import Link from "next/link";
import { useRouter } from "next/router";
import { useAuth } from "@/hooks/useAuth";
import {
  ChartBarIcon,
  BugAntIcon,
  PlusCircleIcon,
  DocumentTextIcon,
  UsersIcon,
  DocumentMagnifyingGlassIcon,
} from "@heroicons/react/24/outline";

const Navbar: React.FC = () => {
  const { user, logout } = useAuth();
  const router = useRouter();

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  // Define navigation based on user role
  const getNavigation = () => {
    const role = user?.role;

    // Admin sees everything
    if (role === "ROLE_ADMIN") {
      return [
        { name: "Dashboard", href: "/dashboard", icon: ChartBarIcon },
        { name: "Issues", href: "/issues", icon: BugAntIcon },
        { name: "Create Issue", href: "/issues/create", icon: PlusCircleIcon },
        // In Navbar.tsx, add to admin navigation
        {
          name: "Dependency Scanner",
          href: "/admin/dependency-scanner",
          icon: DocumentMagnifyingGlassIcon,
        },
        { name: "Reports", href: "/reports", icon: DocumentTextIcon },
        { name: "Users", href: "/users", icon: UsersIcon },
      ];
    }

    // Security Admin (Researcher) sees create issue
    if (role === "ROLE_SECURITY_ADMIN") {
      return [
        { name: "Dashboard", href: "/dashboard", icon: ChartBarIcon },
        { name: "Issues", href: "/issues", icon: BugAntIcon },
        { name: "Create Issue", href: "/issues/create", icon: PlusCircleIcon },
        { name: "Reports", href: "/reports", icon: DocumentTextIcon },
      ];
    }

    // Developer sees only issues (no create issue)
    return [
      { name: "Dashboard", href: "/dashboard", icon: ChartBarIcon },
      { name: "My Issues", href: "/issues", icon: BugAntIcon },
    ];
  };

  const navigation = getNavigation();

  return (
    <>
      <nav className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex">
              <div className="flex-shrink-0 flex items-center">
                <Link
                  href="/dashboard"
                  className="text-xl font-bold text-blue-600"
                >
                  VulnTrack
                </Link>
              </div>
              <div className="hidden sm:ml-6 sm:flex sm:space-x-8">
                {navigation.map((item) => (
                  <Link
                    key={item.name}
                    href={item.href}
                    className={`inline-flex items-center px-1 pt-1 text-sm font-medium ${
                      router.pathname === item.href ||
                      router.pathname.startsWith(item.href + "/")
                        ? "text-blue-600 border-b-2 border-blue-600"
                        : "text-gray-500 hover:text-gray-700 hover:border-b-2 hover:border-gray-300"
                    }`}
                  >
                    <span className="mr-2">
                      <item.icon className="h-5 w-5" />
                    </span>
                    {item.name}
                  </Link>
                ))}
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-sm text-gray-600">
                👤 {user?.username || "User"} (
                {user?.role?.replace("ROLE_", "")})
              </span>
              <button
                onClick={handleLogout}
                className="inline-flex items-center px-3 py-1.5 text-sm font-medium text-red-600 hover:text-red-700"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </nav>
    </>
  );
};

export default Navbar;
