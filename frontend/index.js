const express = require('express');
const app = express();
const path = require('path');
const port = 5500;

app.use(express.static(path.join(__dirname, './')));
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'demo.html'));
});
app.listen(port, () => {
  console.log(path.join(__dirname, './'));
  console.log(`Example app listening at http://localhost:${port}`);
});
