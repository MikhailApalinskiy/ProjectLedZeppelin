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

    <c:set var="PATH_HOME" value="/"/>
    <c:set var="PATH_LOGIN" value="/login"/>
    <c:set var="PATH_REGISTER" value="/register"/>

    <c:set var="PARAM_NEXT" value="next"/>
    <c:set var="PARAM_USER_LOGIN" value="userLogin"/>
    <c:set var="PARAM_PASSWORD" value="password"/>
    <c:set var="PARAM_QUEST_NAME" value="questName"/>

    <c:url var="homeUrl" value="${PATH_HOME}"/>
    <c:url var="loginAction" value="${PATH_LOGIN}"/>
    <c:url var="registerUrl" value="${PATH_REGISTER}">
        <c:param name="${PARAM_NEXT}" value="${param[PARAM_NEXT]}"/>
    </c:url>
</head>
<body class="page-auth">

<c:if test="${not empty param[PARAM_QUEST_NAME]}">
    <c:set var="info" scope="request"
           value="Войдите, чтобы продолжить квест: ${param[PARAM_QUEST_NAME]}"/>
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
                <input type="hidden" name="${PARAM_NEXT}" value="<c:out value='${param[PARAM_NEXT]}'/>"/>

                <div class="field">
                    <label for="login" class="label">Логин</label>
                    <input id="login"
                           name="${PARAM_USER_LOGIN}"
                           class="input"
                           autocomplete="username"
                           autofocus
                           value="<c:out value='${requestScope.userLogin}'/>"/>
                </div>

                <div class="field">
                    <label for="password" class="label">Пароль</label>
                    <input id="password"
                           name="${PARAM_PASSWORD}"
                           type="password"
                           class="input"
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