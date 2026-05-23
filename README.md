# STYLE SPHERE

STYLE SPHERE is a portfolio-ready premium fashion ecommerce platform built to demonstrate a complete online shopping workflow, from product discovery and cart management to Razorpay payments and admin analytics.

STYLE SPHERE is a full-stack fashion ecommerce app with a React + Vite storefront, Java Servlet API backend, JDBC persistence, MySQL catalog/order data, admin catalog management, and Razorpay test-mode checkout integration.

## Project Highlights

- Full-stack ecommerce architecture with clear frontend, backend, and database separation
- Java Servlet backend using JDBC-based data access and Maven build tooling
- React + Vite frontend with responsive shopping, checkout, account, and admin views
- MySQL integration for users, products, carts, orders, order items, and payments
- Razorpay test-mode payment integration with backend signature verification
- Admin analytics dashboard for product count, order count, revenue, customers, payment statuses, and recent orders

## Tech Stack

- Frontend: React 18, Vite, Tailwind CSS, lucide-react
- Backend: Java 17, Jakarta Servlets, Maven, Jetty
- Database: MySQL 8, JDBC
- Payments: Razorpay Checkout + Razorpay Java SDK

## Folder Structure

```text
backend/              Java Servlet API, DAOs, models, Maven config
database/schema.sql   MySQL schema, migrations, categories, product seed data
frontend/             React storefront and admin UI
```

## Setup

Frontend:

```powershell
cd frontend
npm install
npm run dev
npm run build
```

Backend:

```powershell
cd backend
mvn test
mvn jetty:run
```

MySQL:

```powershell
Get-Content .\database\schema.sql | mysql -u root -p
```

The app defaults to:

```text
DB_URL=jdbc:mysql://localhost:3306/style_sphere?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=fashion_app
DB_PASSWORD=fashion_app_password
ADMIN_EMAIL=admin@atelier.local
ADMIN_PASSWORD=Admin@123
RAZORPAY_KEY_ID=your_key_here
RAZORPAY_SECRET=your_secret_here
```

Set Razorpay test keys before starting Jetty:

```powershell
$env:RAZORPAY_KEY_ID="your_key_here"
$env:RAZORPAY_SECRET="your_secret_here"
mvn jetty:run
```

## Features

- Product catalog with Men, Women, Footwear, and Accessories filters
- 18 seeded premium fashion products with Unsplash images
- Product details, cart, checkout, user profile, and order history
- Razorpay test checkout order creation and backend signature verification
- Payment status storage: `payment_id`, `payment_status`, `razorpay_order_id`, `payment_method`
- Admin summary for products, orders, revenue, customers, payment statuses, and recent orders

## Implemented Functionalities

- Authentication
- Cart management
- Checkout flow
- Razorpay payments
- Order history
- Admin dashboard
- Product management
- Category filtering
- Profile management

## API Endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/products` | List products |
| GET | `/api/products?category=Men` | Filter products by category |
| GET | `/api/products/{id}` | Product detail |
| GET/POST/PUT/DELETE | `/api/cart` | Cart operations |
| POST | `/api/auth/register` | Register |
| POST | `/api/auth/login` | Login |
| POST | `/api/auth/logout` | Logout |
| GET/PUT | `/api/profile` | Profile |
| GET | `/api/orders` | Order history |
| POST | `/api/orders` | Create pending order and Razorpay order |
| POST | `/api/payments/verify` | Verify Razorpay signature and mark paid |
| POST | `/api/payments/failure` | Record failed payment |
| GET | `/api/admin/summary` | Dashboard metrics |
| GET/POST | `/api/admin/products` | Admin product list/create |
| PUT/DELETE | `/api/admin/products/{id}` | Admin product update/delete |

## Test Credentials

```text
Admin email: admin@atelier.local
Admin password: Admin@123
```

Register a new customer from the UI for checkout testing.

## Razorpay Test Card

Use Razorpay test mode credentials and the following card details during checkout:

```text
Card number: 4111 1111 1111 1111
Expiry: Any future expiry
CVV: Any CVV
OTP: 1234
```

## Screenshots

Place screenshots here:

```text
docs/screenshots/home.png
docs/screenshots/product-detail.png
docs/screenshots/cart-checkout.png
docs/screenshots/admin-dashboard.png
```

## GitHub Push Steps

```powershell
git init
git add .
git commit -m "Complete ecommerce enhancements and Razorpay integration"
git branch -M main
git remote add origin https://github.com/<user>/<repo>.git
git push -u origin main
```
