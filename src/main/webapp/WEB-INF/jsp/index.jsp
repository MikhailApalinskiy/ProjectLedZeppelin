<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.javarush.apalinskiy.user.User" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="utf-8" />
    <title>Главная</title>
</head>
<body>
<%
    javax.servlet.http.HttpSession s = request.getSession(false);
    User user = (s == null) ? null : (User) s.getAttribute("user");
%>

<h1>Главная</h1>

<% if (user != null) { %>
<p>Вы вошли как: <b><%= user.getUserName() %></b> (логин: <%= user.getUserLogin() %>)</p>
<form method="post" action="<%= request.getContextPath() %>/logout">
    <button type="submit">Выйти</button>
</form>
<% } else { %>
<p>Вы не вошли в систему.</p>
<p>
    <a href="${pageContext.request.contextPath}/login">Войти</a>
    <a href="${pageContext.request.contextPath}/register">Зарегистрироваться</a>
</p>
<% } %>

</body>
</html>