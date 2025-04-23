const dotEnv = require("dotenv");

if (process.env.NODE_ENV == "prod") {
  const configFile = `./.env.production`;
  dotEnv.config({ path: configFile });
} else {
  dotEnv.config();
}

module.exports = {
  PORT: process.env.PORT || 5000,
  DB_URL: process.env.MONGODB_URI || "mongodb://nosql-db/iwaUserDB",
  APP_SECRET: process.env.APP_SECRET || "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
  EXCHANGE_NAME: process.env.EXCHANGE_NAME || "IWA_STORE",
  MSG_QUEUE_URL: process.env.MSG_QUEUE_URL || "amqp://rabbitmq:5672",
  USER_SERVICE: "user_service",
  STORE_SERVICE: "store_service",
};
