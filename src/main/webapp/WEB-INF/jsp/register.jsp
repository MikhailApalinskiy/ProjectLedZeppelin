<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="PATH_HOME" value="/"/>
<c:set var="PATH_REGISTER" value="/register"/>
<c:set var="PATH_LOGIN" value="/login"/>

<c:set var="PATH_CSS_MAIN" value="/assets/css/main.css"/>
<c:set var="PATH_CSS_AUTH" value="/assets/css/auth.css"/>

<c:set var="NEXT_VALUE" value="${param.next}"/>

<!doctype html>
<html lang="ru">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Регистрация — TextQuest</title>

    <c:url var="cssMain" value="${PATH_CSS_MAIN}"/>
    <c:url var="cssAuth" value="${PATH_CSS_AUTH}"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssAuth}"/>
</head>
<body class="page-auth">

<c:url var="homeUrl" value="${PATH_HOME}"/>
<c:url var="registerAction" value="${PATH_REGISTER}"/>
<c:url var="loginUrl" value="${PATH_LOGIN}">
    <c:param name="next" value="${NEXT_VALUE}"/>
</c:url>

<c:if test="${not empty NEXT_VALUE}">
    <c:set var="info" scope="request"
           value="Зарегистрируйтесь, чтобы продолжить: ${NEXT_VALUE}"/>
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
                <input type="hidden" name="next" value="<c:out value='${NEXT_VALUE}'/>"/>

                <div class="field">
                    <label for="userName" class="label">Имя</label>
                    <input id="userName"
                           name="userName"
                           class="input"
                           autocomplete="name"
                           autofocus
                           value="<c:out value='${requestScope.userName}'/>"/>
                </div>

                <div class="field">
                    <label for="userLogin" class="label">Логин</label>
                    <input id="userLogin"
                           name="userLogin"
                           class="input"
                           autocomplete="username"
                           value="<c:out value='${requestScope.userLogin}'/>"/>
                </div>

                <div class="field">
                    <label for="password" class="label">Пароль</label>
                    <input id="password"
                           name="password"
                           type="password"
                           class="input"
                           autocomplete="new-password"/>
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