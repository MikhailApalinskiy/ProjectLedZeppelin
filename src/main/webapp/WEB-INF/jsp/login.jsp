<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>  <%-- ★ добавили --%>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Войти — TextQuest</title>
    <link rel="stylesheet" href="<c:url value='/assets/css/main.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/auth.css'/>">
</head>
<body>
<main class="container fade-in" role="main">
    <article class="card" aria-labelledby="loginTitle">
        <header class="card-header">
            <span class="logo" aria-hidden="true"></span>
            <h1 id="loginTitle">Вход</h1>
        </header>
        <section class="card-body">
            <c:if test="${not empty requestScope.error}">
                <div class="alert alert-error" role="alert">
                    <span class="alert-dot" aria-hidden="true"></span>
                    <span class="alert-text">${requestScope.error}</span>
                </div>
            </c:if>
            <form method="post" action="<c:url value='/login'/>" class="form stack" novalidate>
                <div class="field">
                    <label for="login" class="label">Логин</label>
                    <input id="login" name="userLogin" class="input" required autocomplete="username"
                           value="${fn:escapeXml(requestScope.userLogin)}" />
                </div>
                <div class="field">
                    <label for="password" class="label">Пароль</label>
                    <input id="password" name="password" type="password" class="input" required
                           autocomplete="current-password">
                </div>
                <div class="form-actions">
                    <button type="submit" class="btn btn-primary">Войти</button>
                    <a class="btn btn-ghost" href="<c:url value='/'/>">На главную</a>
                    <a class="link" href="<c:url value='/register'/>">Зарегистрироваться</a>
                </div>
            </form>
        </section>
    </article>
</main>
</body>
</html>