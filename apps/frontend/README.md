# Frontend Application

This directory is reserved for a React-based Single Page Application (SPA) to provide a user interface for the IWA-Microservics microservices.

## Current Status

A simple HTML page is provided in `public/index.html` for API testing and service discovery.

## Future Development

The full React SPA will include:

- Product catalog browsing
- User authentication and registration
- Shopping cart and checkout
- Order management
- Prescription management
- Customer profile management

## Viewing the Current Interface

Open `public/index.html` in a web browser or serve it with a simple HTTP server:

```bash
cd public
python3 -m http.server 3000
```

Then visit: http://localhost:3000

## Security Note

⚠️ The future frontend will also contain intentional vulnerabilities for demonstration purposes, including:
- XSS (Cross-Site Scripting)
- CSRF (Cross-Site Request Forgery)
- Insecure storage of sensitive data
- Client-side logic vulnerabilities
