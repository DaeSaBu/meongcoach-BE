window.onload = function() {
  window.ui = SwaggerUIBundle({
    url: "./openapi3.json",
    dom_id: "#swagger-ui",
    deepLinking: true,
    presets: [SwaggerUIBundle.presets.apis],
    layout: "BaseLayout"
  });
};
