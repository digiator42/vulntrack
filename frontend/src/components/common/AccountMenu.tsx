// src/components/common/AccountMenu.tsx
import React, { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/router";
import {
  ArrowRightOnRectangleIcon,
  BugAntIcon,
  ChevronDownIcon,
  HomeIcon,
  UsersIcon,
} from "@heroicons/react/24/outline";
import { useAuth } from "@/hooks/useAuth";

const ROLE_STYLES: Record<string, string> = {
  ROLE_ADMIN: "bg-red-100 text-red-800",
  ROLE_SECURITY_ADMIN: "bg-amber-100 text-amber-800",
  ROLE_DEVELOPER: "bg-blue-100 text-blue-800",
};

const AVATAR_STYLES: Record<string, string> = {
  ROLE_ADMIN: "bg-red-600",
  ROLE_SECURITY_ADMIN: "bg-amber-600",
  ROLE_DEVELOPER: "bg-blue-600",
};

function initials(name: string): string {
  return name
    .split(/[\s._-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? "")
    .join("");
}

/**
 * Account management dropdown replacing the plain "user (ROLE) + Logout"
 * header. Shows identity, role badge, quick links and sign-out.
 */
const AccountMenu: React.FC = () => {
  const { user, logout } = useAuth();
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const onPointerDown = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, []);

  if (!user) return null;

  const role = user.role ?? "";
  const shortRole = role.replace("ROLE_", "");
  const avatarBg = AVATAR_STYLES[role] ?? "bg-gray-600";
  const badge = ROLE_STYLES[role] ?? "bg-gray-100 text-gray-800";

  const handleLogout = async () => {
    setOpen(false);
    await logout();
    router.push("/login");
  };

  const links =
    role === "ROLE_ADMIN"
      ? [
          { name: "Dashboard", href: "/dashboard", Icon: HomeIcon },
          { name: "My Issues", href: "/issues", Icon: BugAntIcon },
          { name: "Users", href: "/users", Icon: UsersIcon },
        ]
      : [
          { name: "Dashboard", href: "/dashboard", Icon: HomeIcon },
          { name: "My Issues", href: "/issues", Icon: BugAntIcon },
        ];

  return (
    <div ref={rootRef} className="relative">
      <button
        onClick={() => setOpen((o) => !o)}
        aria-haspopup="menu"
        aria-expanded={open}
        className="flex items-center gap-2 rounded-full py-1 pl-1 pr-2 hover:bg-gray-100"
      >
        <span
          className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-bold text-white ${avatarBg}`}
        >
          {initials(user.username || "U")}
        </span>
        <span className="hidden text-left md:block">
          <span className="block max-w-32 truncate text-sm font-medium text-gray-700">
            {user.username}
          </span>
          <span className="block text-[11px] text-gray-500">{shortRole}</span>
        </span>
        <ChevronDownIcon
          className={`h-4 w-4 text-gray-500 transition-transform ${open ? "rotate-180" : ""}`}
        />
      </button>

      {open && (
        <div
          role="menu"
          className="absolute right-0 z-50 mt-2 w-64 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg animate-fade-in"
        >
          <div className="flex items-center gap-3 border-b border-gray-100 px-4 py-3">
            <span
              className={`flex h-10 w-10 items-center justify-center rounded-full text-sm font-bold text-white ${avatarBg}`}
            >
              {initials(user.username || "U")}
            </span>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-gray-900">
                {user.username}
              </p>
              <p className="truncate text-xs text-gray-500">{user.email}</p>
              <span
                className={`mt-1 inline-block rounded-full px-2 py-0.5 text-[11px] font-medium ${badge}`}
              >
                {shortRole}
              </span>
            </div>
          </div>

          <div className="py-1">
            {links.map(({ name, href, Icon }) => (
              <Link
                key={name}
                href={href}
                onClick={() => setOpen(false)}
                className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
              >
                <Icon className="h-4 w-4 text-gray-400" />
                {name}
              </Link>
            ))}
          </div>

          <div className="border-t border-gray-100 py-1">
            <button
              onClick={handleLogout}
              className="flex w-full items-center gap-2 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50"
            >
              <ArrowRightOnRectangleIcon className="h-4 w-4" />
              Logout
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default AccountMenu;
