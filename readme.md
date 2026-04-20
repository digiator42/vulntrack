```bash
vulntrack/                          # Root monorepo
├── backend/                        # Spring Boot API
│   ├── .mvn/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/megastar/vulntrack/
│   │   │   │       ├── config/
│   │   │   │       ├── controller/
│   │   │   │       ├── dto/
│   │   │   │       ├── exception/
│   │   │   │       ├── filter/
│   │   │   │       ├── model/
│   │   │   │       ├── repository/
│   │   │   │       └── service/
│   │   └── test/
│   ├── pom.xml
│   │── application-dev.yml
│   │── application.yml
│   ├── .gitignore
│   └── README.md
│
├── frontend/                       # Next.js App
│   ├── public/
│   │   ├── icons/
│   │   └── images/
│   ├── src/
│   │   ├── app/
│   │   │   ├── (auth)/            # Auth routes (grouped)
│   │   │   │   ├── login/
│   │   │   │   │   └── page.tsx
│   │   │   │   └── register/
│   │   │   │       └── page.tsx
│   │   │   ├── (dashboard)/       # Protected routes (grouped)
│   │   │   │   ├── layout.tsx     # Shared dashboard layout
│   │   │   │   ├── page.tsx       # /dashboard
│   │   │   │   ├── vulnerabilities/
│   │   │   │   │   ├── page.tsx           # List
│   │   │   │   │   ├── [id]/
│   │   │   │   │   │   ├── page.tsx       # Detail
│   │   │   │   │   │   └── edit/page.tsx  # Edit
│   │   │   │   │   └── new/page.tsx       # Create
│   │   │   │   ├── reports/
│   │   │   │   │   └── page.tsx
│   │   │   │   └── settings/
│   │   │   │       └── page.tsx
│   │   │   ├── api/               # API routes
│   │   │   │   └── auth/
│   │   │   │       └── [...nextauth]/route.ts
│   │   │   ├── layout.tsx         # Root layout
│   │   │   └── globals.css
│   │   ├── components/
│   │   │   ├── ui/                # shadcn/ui components
│   │   │   │   ├── button.tsx
│   │   │   │   ├── card.tsx
│   │   │   │   ├── table.tsx
│   │   │   │   └── ...
│   │   │   ├── layout/
│   │   │   │   ├── Header.tsx
│   │   │   │   ├── Sidebar.tsx
│   │   │   │   └── Footer.tsx
│   │   │   ├── vulnerabilities/
│   │   │   │   ├── VulnTable.tsx
│   │   │   │   ├── VulnCard.tsx
│   │   │   │   ├── VulnForm.tsx
│   │   │   │   ├── CVSSCalculator.tsx
│   │   │   │   └── SeverityBadge.tsx
│   │   │   ├── charts/
│   │   │   │   ├── SeverityChart.tsx
│   │   │   │   ├── TrendGraph.tsx
│   │   │   │   └── StatsCard.tsx
│   │   │   └── forms/
│   │   │       └── LoginForm.tsx
│   │   ├── lib/
│   │   │   ├── api/
│   │   │   │   ├── client.ts      # Axios instance
│   │   │   │   ├── vulnerabilities.ts
│   │   │   │   └── auth.ts
│   │   │   ├── hooks/
│   │   │   │   ├── useVulnerabilities.ts
│   │   │   │   └── useAuth.ts
│   │   │   ├── utils.ts
│   │   │   └── constants.ts
│   │   └── types/
│   │       ├── vulnerability.ts
│   │       ├── user.ts
│   │       └── api.ts
│   ├── .env.local
│   ├── .env.example
│   ├── next.config.js
│   ├── tailwind.config.ts
│   ├── tsconfig.json
│   ├── package.json
│   ├── bun.lockb
│   └── README.md
│
├── docs/                           # Shared documentation
│   ├── API.md
│   ├── ARCHITECTURE.md
│   ├── SETUP.md
│   └── screenshots/
│
├── docker/                         # Docker configs
│   ├── backend.Dockerfile
│   ├── frontend.Dockerfile
│   └── nginx.conf
│
├── .github/                        # CI/CD
│   └── workflows/
│       ├── backend-ci.yml
│       └── frontend-ci.yml
│
├── docker-compose.yml              # Local development
├── docker-compose.prod.yml         # Production
├── .gitignore                      # Root gitignore
└── README.md                       # Main README
```