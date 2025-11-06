# Virtual Bank System - Frontend

A modern, fully functional React-based frontend for the Virtual Bank System microservices application.

## 🚀 Features

- **User Authentication**: Secure login and registration
- **Dashboard**: Comprehensive view of all accounts and recent transactions
- **Account Management**: Create savings and checking accounts
- **Money Transfers**: Initiate and execute transfers between accounts
- **Real-time Updates**: Refresh dashboard to see latest balances and transactions
- **Responsive Design**: Works seamlessly on desktop and mobile devices
- **Modern UI**: Beautiful, professional interface with Tailwind CSS

## 🛠️ Technology Stack

- **React 18** - Modern UI library
- **TypeScript** - Type-safe development
- **Vite** - Lightning-fast build tool
- **React Router** - Client-side routing
- **Zustand** - Lightweight state management
- **Axios** - HTTP client for API calls
- **Tailwind CSS** - Utility-first CSS framework
- **React Hot Toast** - Beautiful notifications
- **Lucide React** - Modern icon library

## 📋 Prerequisites

- Node.js 18+ and npm
- All backend services running:
  - BFF Service (port 8080)
  - User Service (port 8081)
  - Account Service (port 8082)
  - Transaction Service (port 8083)

## 🏃 Getting Started

### Installation

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install
```

### Running the Application

```bash
# Start development server
npm run dev
```

The application will be available at `http://localhost:3000`

### Building for Production

```bash
# Create production build
npm run build

# Preview production build
npm run preview
```

## 📁 Project Structure

```
frontend/
├── src/
│   ├── components/          # Reusable components
│   │   └── ProtectedRoute.tsx
│   ├── pages/              # Page components
│   │   ├── Login.tsx
│   │   ├── Register.tsx
│   │   ├── Dashboard.tsx
│   │   ├── CreateAccount.tsx
│   │   └── Transfer.tsx
│   ├── services/           # API services
│   │   └── api.ts
│   ├── store/              # State management
│   │   └── authStore.ts
│   ├── types/              # TypeScript types
│   │   └── api.ts
│   ├── utils/              # Utility functions
│   │   └── format.ts
│   ├── App.tsx             # Main app component
│   ├── main.tsx            # Entry point
│   └── index.css           # Global styles
├── public/                 # Static assets
├── index.html             # HTML template
├── vite.config.ts         # Vite configuration
├── tailwind.config.js     # Tailwind configuration
└── package.json           # Dependencies
```

## 🎯 User Guide

### Getting Started

1. **Register a New Account**
   - Click "Sign up" on the login page
   - Fill in your personal information
   - Create a username and password
   - Submit the form

2. **Login**
   - Enter your username and password
   - Click "Sign In"

3. **Create a Bank Account**
   - From the dashboard, click "Create New Account"
   - Choose account type (Savings or Checking)
   - Enter initial balance
   - Submit the form

4. **Make a Transfer**
   - Click "Transfer Money" from the dashboard
   - Select the source account
   - Enter the recipient account ID (UUID)
   - Enter the amount and optional description
   - Confirm the transfer

5. **View Transactions**
   - All recent transactions are displayed on the dashboard
   - Each account shows its latest transactions
   - Use the refresh button to update data

## 🔌 API Integration

The frontend integrates with the following backend services:

- **BFF Service** (`http://localhost:8080/bff`)
  - Dashboard data aggregation

- **User Service** (`http://localhost:8081/users`)
  - User registration
  - User login
  - User profile

- **Account Service** (`http://localhost:8082`)
  - Create accounts
  - View account details
  - Get user accounts

- **Transaction Service** (`http://localhost:8083`)
  - Initiate transfers
  - Execute transfers
  - View transaction history

### CORS Handling

The Vite development server is configured with proxy rules to handle CORS issues during development. All API calls are proxied through the Vite server.

## 🎨 UI/UX Features

- **Consistent Design**: Professional banking interface
- **Responsive Layout**: Works on all screen sizes
- **Loading States**: Clear feedback during operations
- **Error Handling**: User-friendly error messages
- **Toast Notifications**: Real-time feedback for actions
- **Protected Routes**: Automatic redirect for unauthenticated users
- **Persistent Auth**: Login state persists across sessions

## 🔐 Security

- Client-side authentication state management
- Protected routes for authenticated pages
- Secure form handling
- Input validation
- Error message sanitization

## 🧪 Development

### Available Scripts

```bash
# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

### Environment Variables

Create a `.env` file for custom configuration:

```env
VITE_BFF_URL=http://localhost:8080/bff
VITE_USER_URL=http://localhost:8081/users
VITE_ACCOUNT_URL=http://localhost:8082
VITE_TRANSACTION_URL=http://localhost:8083
```

## 🐛 Troubleshooting

### Backend Connection Issues

If you encounter connection errors:

1. Ensure all backend services are running
2. Check service ports match the configuration
3. Verify CORS settings on backend services
4. Check browser console for detailed errors

### Build Issues

```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Clear Vite cache
rm -rf .vite
npm run dev
```

## 📝 API Type Definitions

All API types are defined in `src/types/api.ts` and match the OpenAPI specification from the backend.

## 🤝 Contributing

1. Follow the existing code structure
2. Use TypeScript for type safety
3. Follow the component design patterns
4. Write descriptive commit messages

## 📄 License

This project is part of the Virtual Bank System microservices architecture.

---

Built with ❤️ using React, TypeScript, and Vite
