# WordPress Migration Assessment

Source: `C:\Users\swami\Local Sites\chatlaorganics`

## Inventory completed

- Local domain: `chatlaorganics.local`
- PHP: 8.2.29
- MySQL: 8.4.0
- Database export: `app/sql/local.sql` (about 10 MB)
- Uploads: 257 files, about 56 MB
- Content snapshot: 10 pages, 3 posts, 24 attachments
- Plugins present: WooCommerce, Elementor, Rank Math/Yoast components, Cookiez, image optimization, accessibility, Astra Sites, and the custom Angie plugin
- Custom plugin: Angie is primarily an administration/AI and deployment utility, not the storefront domain model

## Architecture decision

Start with a modular monolith, not microservices. A business website with a currently small dataset benefits more from one deployable application, one database, one authentication boundary, and straightforward local debugging. Modules will have explicit package boundaries:

- `content`: pages, posts, navigation, SEO metadata
- `catalog`: products, categories, inventory, pricing
- `orders`: cart, checkout, orders, payments, fulfillment
- `identity`: customers, staff roles, sessions
- `media`: attachment metadata and storage references

Extract a service only when a module has independent scaling, deployment, ownership, or security requirements.

## Migration sequence

1. Preserve WordPress and export database/media backups.
2. Build API health check and database migrations.
3. Import pages/posts/media into `content` and `media`, preserving slugs and old URLs.
4. Build the React public shell and responsive navigation.
5. Add catalog and product import from WooCommerce tables.
6. Add cart, checkout, payment provider, order administration, email, and tax/shipping rules.
7. Port SEO metadata, sitemap, robots rules, cookie consent, redirects, analytics, and accessibility checks.
8. Run a parallel staging site and compare every URL, asset, and checkout flow.
9. Deploy behind HTTPS and a CDN, then switch DNS only after rollback is tested.

## Open decisions to confirm during implementation

- Product, price, inventory, tax, shipping, and payment requirements
- Whether Elementor layouts need pixel-level reproduction or content-only migration
- Email provider and transactional templates
- Production host and domain name
- Admin roles and whether customer accounts must be retained

The current export has no populated `product` or `shop_order` records, so catalog and checkout are prepared as modules but should not be assumed complete until the live business data is confirmed.

## Product vision in simple language

Chatla Organics should have two different experiences:

### Customer website

Customers should be able to open the site on a phone, tablet, or computer and shop without creating an account.

- Home page with the brand story and featured products
- Simple product list with search, categories, price, stock status, and product photos
- Product detail page with quantity and add-to-cart
- Small cart drawer or cart page
- Guest checkout form: name, phone, email, address, delivery notes, and preferred contact method
- Payment choice: begin with cash on delivery or a configured payment provider
- Order confirmation page and confirmation message
- No login required for the first release
- Order lookup later using order number plus phone/email, if needed

The checkout must validate required fields, calculate the final total on the server, prevent overselling, and show a clear success or failure message. Customer data must be stored securely and never trusted directly from browser totals.

### Admin website

The admin area should be a separate protected UI at `/admin`, with a different layout from the public website. Only staff users can access it.

First admin navigation:

- Dashboard: new orders, sales total, low-stock products, and unread notifications
- Products: add, edit, publish/unpublish, archive, upload photos, set SKU, price, sale price, and stock
- Categories: create and organize product categories
- Orders: see new orders, customer details, items, totals, notes, and status
- Order actions: confirm, pack, ship, complete, cancel, and record payment status
- Notifications: new-order alerts and low-stock alerts
- Content: edit home-page sections, pages, contact information, and announcement banners
- Settings: business contact details, delivery areas, payment methods, tax, and notification recipients
- Staff: add or disable staff accounts and assign permissions later

The product form should be simple at first: name, description, image, category, price, sale price, stock quantity, SKU, and publish status. Variants, bulk editing, coupons, multiple warehouses, and advanced pricing can be added only when the business needs them.

## Recommendations from established open-source projects

Reviewed for patterns:

- [Medusa](https://github.com/medusajs/medusa): strongest reference for clear commerce modules, server-side pricing, inventory, orders, fulfillment, and a separate admin dashboard. Borrow its domain boundaries and explicit admin/store API separation.
- [Saleor Dashboard](https://github.com/saleor/saleor-dashboard): strongest reference for a permission-aware, feature-based React admin. Borrow list screens with search, pagination, empty states, loading states, and separate product/order sections.
- [Vendure](https://github.com/vendure-ecommerce/vendure): useful reference for an extensible TypeScript commerce core and admin extensions. Its plugin model is more than the first Chatla release needs, but the extension boundary is a good future idea.
- [Bagisto](https://github.com/bagisto/bagisto): useful reference for conventional catalog administration and guest checkout. Its guest checkout and admin catalog settings match the desired customer simplicity, although adopting the whole PHP platform would conflict with the planned Java backend.

### Decision

Build a custom Chatla admin and customer UI on the existing Spring Boot and React foundation. Do not copy an entire open-source commerce platform. Use the ideas above as design references and keep the first release small enough for one business owner to operate.

## Proposed technical components

### Backend components

- Spring Boot REST API with versioned routes such as `/api/v1/products` and `/api/v1/orders`
- MySQL 8 as the primary relational database
- Flyway for every schema change
- Spring Security for staff authentication and role checks
- Bean Validation for all incoming forms
- Server-side order totals, stock reservation, and order state transitions
- Notification adapter for email first, with WhatsApp/SMS adapter later
- Media storage interface: local files during development, object storage/CDN in production
- Audit fields and an admin activity log for price, stock, and order changes
- Actuator health and metrics endpoints, with secrets kept in environment variables

### Frontend components

- Public React storefront optimized for mobile first
- Product listing, product detail, cart, guest checkout, and confirmation views
- Protected React admin application with its own navigation and layout
- Reusable form, table, status badge, modal, notification, and empty-state components
- API client with consistent loading, validation, and error handling
- Responsive image handling and accessible labels, focus states, and keyboard navigation
- Playwright tests for guest checkout and the critical admin product/order flows

### Database components

Initial tables already created:

- `content_pages`
- `media_assets`
- `flyway_schema_history`

Next tables:

- `products`, `product_categories`, `product_images`
- `inventory_items` or stock fields on product variants
- `orders`, `order_items`, `order_addresses`, `order_status_history`
- `admin_users`, `admin_roles`, `admin_sessions`
- `notifications`, `notification_deliveries`
- `site_settings`, `redirects`, and SEO metadata

Keep money as decimal values with an explicit currency, keep status changes auditable, and use database constraints for slugs, SKUs, and order numbers.

## Notifications design

When a guest submits an order:

1. The API validates the form and creates the order in one transaction.
2. The API reserves or reduces stock.
3. The customer sees an order number immediately.
4. The admin dashboard shows a new-order badge.
5. The server sends an email to the business and a confirmation to the customer.
6. A failed notification is retried and recorded; it must not silently lose the order.

Start with email through a transactional provider or local Mailpit. Add WhatsApp/SMS only after the order workflow is stable and provider costs are understood.

## Future improvement path

### Phase 1: working foundation

- Complete product CRUD in the admin UI
- Add public product listing and product details
- Add guest cart and checkout
- Add order persistence and email notifications
- Add basic staff login and protected admin routes

### Phase 2: operating the business

- Order status workflow and printable packing view
- Stock alerts and low-stock thresholds
- Product categories, search, sale prices, and delivery rules
- Content editor for pages and homepage sections
- SEO metadata, sitemap, redirects, analytics, cookie consent, and backups

### Phase 3: growth

- Payment gateway and webhook verification
- Coupons, bundles, product variants, and scheduled pricing
- Customer order lookup and optional accounts
- Reports, CSV import/export, and staff roles
- Object storage/CDN, caching, background jobs, and monitoring

### Phase 4: only if the business needs it

- Extract catalog, order, or notification modules into services
- Add a message broker for asynchronous workflows
- Add search infrastructure such as OpenSearch when MySQL search is no longer enough
- Add multiple stores, warehouses, currencies, or sales channels

Microservices are a later optimization. The first production goal is a reliable single deployment with backups, HTTPS, monitoring, and a tested rollback.

## Current implementation checklist

- [x] WordPress files, plugins, uploads, and SQL export inventoried
- [x] Modular-monolith architecture selected
- [x] Spring Boot application created
- [x] Java 21 and Maven verified locally
- [x] MySQL connection to Local verified
- [x] Flyway baseline and first migration applied
- [x] API health endpoint working at `http://localhost:8080/api/health`
- [x] React/Vite storefront shell created
- [x] Customer-facing responsive layout created
- [x] Chatla Organics logo added to the navbar
- [x] Separate admin layout at `/admin` (authentication still pending)
- [x] Product CRUD API and admin UI (add, edit, price/stock/category update, delete)
- [x] Public catalog and product cards
- [x] Real Mango, Lemon, and Tomato Pickle photos connected to the storefront
- [x] Refined storefront typography and product-card presentation
- [x] Guided guest checkout UI: phone, local demo OTP, address, payment choice, confirmation
- [ ] Server-side OTP verification and order creation
- [ ] Admin email notification to `chatlaorganics@gmail.com` after a saved order
- [x] Customer help chatbot with common answers and direct email contact
- [ ] Orders and notifications
- [ ] Content/media import from WordPress
- [ ] SEO and production deployment
