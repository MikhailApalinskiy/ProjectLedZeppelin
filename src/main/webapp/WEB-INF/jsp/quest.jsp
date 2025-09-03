<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="isAuth" value="${not empty sessionScope.user}"/>
<c:choose>
    <c:when test="${node == null}">
        <c:set var="pageTitle" value="TextQuest — Ошибка"/>
    </c:when>
    <c:when test="${node.fin}">
        <c:set var="pageTitle" value="TextQuest — Финал #${node.id}"/>
    </c:when>
    <c:otherwise>
        <c:set var="pageTitle" value="TextQuest — Ветка #${node.id}"/>
    </c:otherwise>
</c:choose>

<!doctype html>
<html lang="ru">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title><c:out value="${pageTitle}"/></title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssQuest" value="/assets/css/quest.css"/>

    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssQuest}"/>

</head>
<body>

<c:url var="questAction" value="/quest"/>
<c:url var="placeholder" value="/assets/img/placeholder.jpg"/>
<c:url var="homeUrl" value="/"/>

<c:url var="saveUrl" value="/saves">
    <c:param name="next" value="/quest"/>
    <c:param name="purpose" value="save"/>
    <c:param name="node" value="${node != null ? node.id : 0}"/>
</c:url>
<c:url var="loadUrl" value="/loads">
    <c:param name="next" value="/quest"/>
    <c:param name="purpose" value="load"/>
    <c:param name="node" value="${node != null ? node.id : 0}"/>
</c:url>

<c:url var="nextQuestUrl" value="/quest">
    <c:param name="id" value="${node != null ? node.id : 0}"/>
</c:url>
<c:url var="loginUrl" value="/login">
    <c:param name="next" value="${nextQuestUrl}"/>
</c:url>
<c:url var="registerUrl" value="/register">
    <c:param name="next" value="${nextQuestUrl}"/>
</c:url>

<div class="container">
    <div class="card fade-in">
        <div class="card-header">
            <div class="logo"></div>
            <h1>Текстовый квест</h1>
        </div>

        <div class="card-body">

            <div class="actions" style="justify-content:flex-end; margin-bottom:10px">
                <c:choose>
                    <c:when test="${isAuth}">
                        <a class="btn" href="${saveUrl}">Сохранить</a>
                        <a class="btn" href="${loadUrl}">Загрузить</a>
                    </c:when>
                    <c:otherwise>
                        <a class="btn" href="#authRequired">Сохранить</a>
                        <a class="btn" href="#authRequired">Загрузить</a>
                    </c:otherwise>
                </c:choose>
                <a class="btn btn-ghost" href="${homeUrl}">На главную</a>
            </div>

            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <c:choose>
                <c:when test="${node == null}">
                    <div class="stack" style="text-align:center">
                        <h2>Узел не найден</h2>
                        <p class="notice">Попробуйте начать заново.</p>
                        <a class="btn btn-primary" href="${questAction}">Начать с начала</a>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="stack">
                        <c:choose>
                            <c:when test="${not empty node.image}">
                                <c:set var="rawImg" value="${node.image}"/>
                                <c:choose>
                                    <c:when test="${fn:startsWith(rawImg, '/')}">
                                        <c:url var="imgSrc" value="${rawImg}"/>
                                    </c:when>
                                    <c:otherwise>
                                        <c:url var="imgSrc" value="/${rawImg}"/>
                                    </c:otherwise>
                                </c:choose>
                            </c:when>
                            <c:otherwise>
                                <c:set var="imgSrc" value="${placeholder}"/>
                            </c:otherwise>
                        </c:choose>

                        <img class="quest-image"
                             src="${imgSrc}"
                             alt="Сцена узла #${node.id}"
                             loading="lazy"
                             onerror="this.onerror=null; this.src='${placeholder}'"/>

                        <div class="panel">
                            <div class="max-ch">
                                <h2>Ветка #<c:out value="${node.id}"/></h2>
                                <p><c:out value="${node.text}"/></p>
                            </div>
                        </div>

                        <c:choose>
                            <c:when test="${node.fin}">
                                <div class="actions" style="justify-content:center">
                                    <a class="btn btn-primary" href="${questAction}">Начать с начала</a>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <form class="form stack" method="post" action="${questAction}">
                                    <input type="hidden" name="fromId" value="${node.id}"/>
                                    <div class="answers">
                                        <c:forEach items="${node.options}" var="opt">
                                            <button class="btn" type="submit" name="answer" value="${opt.choice}">
                                                <c:out value="${opt.choice}"/>
                                            </button>
                                        </c:forEach>
                                    </div>
                                </form>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<div id="authRequired" class="modal" role="dialog" aria-modal="true" aria-labelledby="authTitle">
    <a class="modal__overlay" href="#"></a>
    <div class="modal__card">
        <header class="modal__header">
            <h2 id="authTitle" class="user-title" style="margin:0">Требуется вход</h2>
            <a href="#" class="modal__close btn btn-ghost" aria-label="Закрыть">×</a>
        </header>
        <div class="modal__body">
            <div class="alert alert-error" role="alert">
                <span class="alert-dot" aria-hidden="true"></span>
                <span class="alert-text">Чтобы сохранять и загружать прогресс, войдите в аккаунт.</span>
            </div>
            <p class="notice">Вы можете продолжить прохождение без сохранений.</p>
        </div>
        <div class="modal__actions">
            <a href="${loginUrl}" class="btn btn-primary">Войти</a>
            <a href="${registerUrl}" class="btn">Зарегистрироваться</a>
            <a href="#" class="btn btn-ghost">Позже</a>
        </div>
    </div>
</div>

</body>
</html>