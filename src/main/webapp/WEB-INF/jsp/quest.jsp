<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="isAuth" value="${not empty sessionScope.user}"/>
<c:set var="custom" value="${requestScope.custom}"/>
<c:set var="isCustom" value="${not empty custom}"/>

<c:set var="PATH_HOME" value="/"/>
<c:set var="PATH_QUEST" value="/quest"/>
<c:set var="PATH_SAVES" value="/saves"/>
<c:set var="PATH_LOADS" value="/loads"/>
<c:set var="PATH_LOGIN" value="/login"/>
<c:set var="PATH_REGISTER" value="/register"/>
<c:set var="PATH_CSS_MAIN" value="/assets/css/main.css"/>
<c:set var="PATH_CSS_QUEST" value="/assets/css/quest.css"/>
<c:set var="PATH_PLACEHOLDER" value="/assets/img/placeholder.jpg"/>

<c:set var="PARAM_CUSTOM" value="custom"/>
<c:set var="PARAM_NEXT" value="next"/>
<c:set var="PARAM_PURPOSE" value="purpose"/>
<c:set var="PARAM_NODE" value="node"/>
<c:set var="PARAM_ID" value="id"/>
<c:set var="PARAM_FROM_ID" value="fromId"/>
<c:set var="PARAM_ANSWER" value="answer"/>

<c:set var="PURPOSE_SAVE" value="save"/>
<c:set var="PURPOSE_LOAD" value="load"/>

<c:set var="nodeIdSafe" value="${node != null ? node.id : 0}"/>
<c:set var="isFin" value="${node != null and node.fin}"/>

<c:choose>
    <c:when test="${node == null}">
        <c:set var="pageTitle" value="TextQuest — Ошибка"/>
    </c:when>
    <c:when test="${isFin}">
        <c:set var="pageTitle" value="TextQuest — Финал #${nodeIdSafe}${isCustom ? ' (custom)' : ''}"/>
    </c:when>
    <c:otherwise>
        <c:set var="pageTitle" value="TextQuest — Ветка #${nodeIdSafe}${isCustom ? ' (custom)' : ''}"/>
    </c:otherwise>
</c:choose>

<!doctype html>
<html lang="ru">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title><c:out value="${pageTitle}"/></title>

    <c:url var="cssMain" value="${PATH_CSS_MAIN}"/>
    <c:url var="cssQuest" value="${PATH_CSS_QUEST}"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssQuest}"/>

    <c:url var="questAction" value="${PATH_QUEST}">
        <c:if test="${isCustom}">
            <c:param name="${PARAM_CUSTOM}" value="${custom}"/>
        </c:if>
    </c:url>
    <c:url var="placeholder" value="${PATH_PLACEHOLDER}"/>
    <c:url var="homeUrl" value="${PATH_HOME}"/>

    <c:url var="nextQuestUrl" value="${PATH_QUEST}">
        <c:param name="${PARAM_ID}" value="${nodeIdSafe}"/>
        <c:if test="${isCustom}">
            <c:param name="${PARAM_CUSTOM}" value="${custom}"/>
        </c:if>
    </c:url>

    <c:url var="saveUrl" value="${PATH_SAVES}">
        <c:param name="${PARAM_NEXT}" value="${nextQuestUrl}"/>
        <c:param name="${PARAM_PURPOSE}" value="${PURPOSE_SAVE}"/>
        <c:param name="${PARAM_NODE}" value="${nodeIdSafe}"/>
        <c:if test="${isCustom}">
            <c:param name="${PARAM_CUSTOM}" value="${custom}"/>
        </c:if>
    </c:url>

    <c:url var="loadUrl" value="${PATH_LOADS}">
        <c:param name="${PARAM_NEXT}" value="${nextQuestUrl}"/>
        <c:param name="${PARAM_PURPOSE}" value="${PURPOSE_LOAD}"/>
        <c:param name="${PARAM_NODE}" value="${nodeIdSafe}"/>
    </c:url>

    <c:url var="loginUrl" value="${PATH_LOGIN}">
        <c:param name="${PARAM_NEXT}" value="${nextQuestUrl}"/>
        <c:if test="${not empty questTitle}">
            <c:param name="questName" value="${questTitle}"/>
        </c:if>
    </c:url>
    <c:url var="registerUrl" value="${PATH_REGISTER}">
        <c:param name="${PARAM_NEXT}" value="${nextQuestUrl}"/>
    </c:url>
</head>
<body class="page-quest">

<div class="container">
    <div class="card fade-in">
        <div class="card-header">
            <div class="logo" aria-hidden="true"></div>
            <h1>
                Текстовый квест
                <c:if test="${isCustom}">
                    <span class="badge">custom</span>
                </c:if>
            </h1>
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
                             alt="Сцена узла #${nodeIdSafe}"
                             loading="lazy"
                             onerror="this.onerror=null; this.src='${placeholder}'"/>

                        <div class="panel">
                            <div class="max-ch">
                                <h2>Ветка #<c:out value="${nodeIdSafe}"/></h2>
                                <p><c:out value="${node.text}"/></p>
                            </div>
                        </div>

                        <c:choose>
                            <c:when test="${isFin}">
                                <div class="actions" style="justify-content:center">
                                    <a class="btn btn-primary" href="${questAction}">Начать с начала</a>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <form class="form stack" method="post" action="${questAction}">
                                    <input type="hidden" name="${PARAM_FROM_ID}" value="${nodeIdSafe}"/>
                                    <c:if test="${isCustom}">
                                        <input type="hidden" name="${PARAM_CUSTOM}" value="${custom}"/>
                                    </c:if>
                                    <div class="answers">
                                        <c:forEach items="${node.options}" var="opt">
                                            <button class="btn" type="submit" name="${PARAM_ANSWER}"
                                                    value="${opt.choice}">
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
            <h2 id="authTitle" class="user-title">Требуется вход</h2>
            <a href="#" class="modal__close btn btn-ghost" aria-label="Закрыть">×</a>
        </header>
        <div class="modal__body">
            <div class="alert alert-error" role="alert">
                <span class="alert-dot" aria-hidden="true"></span>
                <span class="alert-text">Чтобы сохранять и загружать прогресс, войдите в аккаунт.</span>
            </div>
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