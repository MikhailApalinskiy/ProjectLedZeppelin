<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="ru">
<head><meta charset="utf-8"><title>Войти</title></head>
<body>
<h1>Вход</h1>
<% String err = (String) request.getAttribute("error"); if (err != null) { %>
<div style="color:red"><%= err %></div>
<% } %>
<form method="post" action="login">
  <label>Логин: <input name="userLogin" required></label><br>
  <label>Пароль: <input name="password" type="password" required></label><br>
  <button type="submit">Войти</button>
</form>
</body>
</html>