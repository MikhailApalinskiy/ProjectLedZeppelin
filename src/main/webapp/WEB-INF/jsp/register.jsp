<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Регистрация — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssAuth" value="/assets/css/auth.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssAuth}"/>
</head>
<body>

<c:url var="homeUrl" value="/"/>
<c:url var="registerAction" value="/register"/>
<c:url var="loginUrl" value="/login">
    <c:param name="next" value="${param.next}"/>
</c:url>

<c:if test="${not empty param.next}">
    <c:set var="info" scope="request" value="Зарегистрируйтесь, чтобы продолжить: ${param.next}"/>
</c:if>

<main class="container fade-in" role="main">
    <article class="card" aria-labelledby="registerTitle">
        <header class="card-header">
            <span class="logo" aria-hidden="true"></span>
            <h1 id="registerTitle">Регистрация</h1>
        </header>
        <section class="card-body">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <form method="post" action="${registerAction}" class="form stack" novalidate>
                <input type="hidden" name="next" value="<c:out value='${param.next}'/>"/>

                <div class="field">
                    <label for="userName" class="label">Имя</label>
                    <input id="userName"
                           name="userName"
                           class="input"
                           required
                           autocomplete="name"
                           autofocus
                           value="<c:out value='${requestScope.userName}'/>"/>
                </div>

                <div class="field">
                    <label for="userLogin" class="label">Логин</label>
                    <input id="userLogin"
                           name="userLogin"
                           class="input"
                           required
                           autocomplete="username"
                           value="<c:out value='${requestScope.userLogin}'/>"/>
                </div>

                <div class="field">
                    <label for="password" class="label">Пароль</label>
                    <input id="password"
                           name="password"
                           type="password"
                           class="input"
                           required
                           autocomplete="new-password"
                           minlength="6"/>
                </div>

                <div class="form-actions">
                    <button type="submit" class="btn btn-primary">Создать аккаунт</button>
                    <a class="btn btn-ghost" href="${homeUrl}">На главную</a>
                    <a class="link" href="${loginUrl}">У меня уже есть аккаунт</a>
                </div>
            </form>
        </section>
    </article>
</main>
</body>
</html>