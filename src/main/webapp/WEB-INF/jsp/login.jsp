<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Войти — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssAuth" value="/assets/css/auth.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssAuth}"/>
</head>
<body>

<c:url var="homeUrl" value="/"/>
<c:url var="loginAction" value="/login"/>
<c:url var="registerUrl" value="/register">
    <c:param name="next" value="${param.next}"/>
</c:url>

<c:if test="${not empty param.next}">
    <c:set var="info" scope="request" value="Войдите, чтобы продолжить: ${param.next}"/>
</c:if>

<main class="container fade-in" role="main">
    <article class="card" aria-labelledby="loginTitle">
        <header class="card-header">
            <span class="logo" aria-hidden="true"></span>
            <h1 id="loginTitle">Вход</h1>
        </header>
        <section class="card-body">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <form method="post" action="${loginAction}" class="form stack" novalidate>
                <input type="hidden" name="next" value="<c:out value='${param.next}'/>"/>

                <div class="field">
                    <label for="login" class="label">Логин</label>
                    <input id="login"
                           name="userLogin"
                           class="input"
                           required
                           autocomplete="username"
                           autofocus
                           value="<c:out value='${requestScope.userLogin}'/>"/>
                </div>

                <div class="field">
                    <label for="password" class="label">Пароль</label>
                    <input id="password"
                           name="password"
                           type="password"
                           class="input"
                           required
                           autocomplete="current-password"/>
                </div>

                <div class="form-actions">
                    <button type="submit" class="btn btn-primary">Войти</button>
                    <a class="btn btn-ghost" href="${homeUrl}">На главную</a>
                    <a class="link" href="${registerUrl}">Зарегистрироваться</a>
                </div>
            </form>
        </section>
    </article>
</main>
</body>
</html>