import Link from "next/link";
import { useRouter } from "next/router";
import { useAuth } from "@/hooks/useAuth";
import AccountMenu from "./AccountMenu";
import ThemeToggle from "./ThemeToggle";
import {
  ChartBarIcon,
  BugAntIcon,
  PlusCircleIcon,
  DocumentTextIcon,
  UsersIcon,
  DocumentMagnifyingGlassIcon,
} from "@heroicons/react/24/outline";

const Navbar: React.FC = () => {
  const { user } = useAuth();
  const router = useRouter();

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

    if (role === "ROLE_DEVELOPER") {
      return [
        { name: "Dashboard", href: "/dashboard", icon: ChartBarIcon },
        { name: "My Issues", href: "/issues", icon: BugAntIcon },
      ];
    }

    // Developer sees only issues (no create issue)
    return [];
  };

  const navigation = getNavigation();

  // Highlight only the best match: on /issues/create both "/issues" and
  // "/issues/create" prefix-match, so pick the longest href.
  const activeHref = navigation
    .filter(
      (item) =>
        router.pathname === item.href ||
        router.pathname.startsWith(item.href + "/"),
    )
    .reduce<string | null>(
      (best, item) =>
        best === null || item.href.length > best.length ? item.href : best,
      null,
    );

  return (
    <>
      <nav className="bg-white shadow-sm border-b border-gray-200 dark:bg-gray-900 dark:border-gray-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex">
              <div className="flex-shrink-0 flex items-center">
                <Link
                  href="/dashboard"
                  className="text-xl font-bold text-blue-600 dark:text-blue-400"
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
                      activeHref === item.href
                        ? "text-blue-600 border-b-2 border-blue-600 dark:text-blue-400 dark:border-blue-400"
                        : "text-gray-500 hover:text-gray-700 hover:border-b-2 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-200 dark:hover:border-gray-600"
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
            {/* Account */}
            {user ? (
              <div className="flex items-center gap-1">
                <ThemeToggle />
                <AccountMenu />
              </div>
            ) : (
              <div className="flex items-center space-x-4">
                <ThemeToggle />
                <Link
                  href="/login"
                  className="inline-flex items-center border rounded border-white px-6 py-1.5 text-sm font-medium text-white bg-blue-600 hover:bg-blue-800"
                >
                  Login
                </Link>
              </div>
            )}
          </div>
        </div>
      </nav>
    </>
  );
};

export default Navbar;
