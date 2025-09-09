<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <title>404 — Страница не найдена</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssError" value="/assets/css/error.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssError}"/>

    <meta name="viewport" content="width=device-width, initial-scale=1"/>
</head>
<body>
<div class="container">

    <c:set var="PATH_HOME" value="/"/>

    <c:url var="homeUrl" value="${PATH_HOME}"/>

    <c:set var="reqUri" value="${requestScope['javax.servlet.error.request_uri']}"/>
    <c:set var="errMsg" value="${requestScope['javax.servlet.error.message']}"/>

    <section class="card error-hero" role="alert" aria-labelledby="errTitle">
        <div class="error-code" aria-hidden="true">404</div>
        <h1 id="errTitle" class="error-title">Страница не найдена</h1>
        <p class="muted error-meta">
            Запрошенный адрес:
            <code><c:out value="${empty reqUri ? '—' : reqUri}"/></code>
            <c:if test="${not empty errMsg}">
                <br/>Сообщение: <code><c:out value="${errMsg}"/></code>
            </c:if>
        </p>

        <nav class="actions" aria-label="Навигация по сайту">
            <a class="btn btn-primary" href="${homeUrl}">На главную</a>
        </nav>
    </section>

</div>
</body>
</html>