<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Модерация квестов — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssMod" value="/assets/css/moderation.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssMod}"/>
</head>
<body class="page-moderation">
<div class="container grid">

    <c:set var="PATH_HOME" value="/"/>
    <c:set var="PATH_MOD" value="/quests/moderation"/>
    <c:url var="homeUrl" value="${PATH_HOME}"/>
    <c:url var="modAction" value="${PATH_MOD}"/>

    <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

    <header class="card page-header">
        <div class="page-header__text">
            <h1 class="page-header__title">Модерация квестов</h1>
            <p class="page-header__sub muted">Новые публикации и правки, ожидающие вашего решения.</p>
        </div>
        <nav class="actions">
            <a class="btn btn-primary" href="${homeUrl}">На главную</a>
        </nav>
    </header>

    <section class="card">
        <div class="section-header">
            <h2 class="section-title">Новые квесты</h2>
            <c:if test="${not empty pendingNew}">
                <span class="badge"><c:out value="${fn:length(pendingNew)}"/></span>
            </c:if>
        </div>

        <c:choose>
            <c:when test="${empty pendingNew}">
                <p class="muted">Очередь новых публикаций пуста.</p>
            </c:when>
            <c:otherwise>
                <table class="list table" aria-label="Очередь новых публикаций">
                    <thead>
                    <tr>
                        <th>Название</th>
                        <th>Автор (login)</th>
                        <th>Отправлено</th>
                        <th class="col-actions">Действия</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="pn" items="${pendingNew}">
                        <tr>
                            <td><c:out value="${pn.name}"/></td>
                            <td><code><c:out value="${pn.ownerLogin}"/></code></td>
                            <td><code><c:out value="${pn.submittedAt}"/></code></td>
                            <td class="col-actions td-actions">
                                <form method="post" action="${modAction}" class="actions actions--row">
                                    <input type="hidden" name="id" value="${pn.pendingId}"/>
                                    <c:url var="previewUrl" value="/quests/moderation/preview">
                                        <c:param name="kind" value="new"/>
                                        <c:param name="id" value="${pn.pendingId}"/>
                                    </c:url>
                                    <a class="btn btn-ghost" href="${previewUrl}">Просмотр</a>
                                    <button class="btn btn-primary" name="action" value="approveCreate" type="submit">
                                        Одобрить
                                    </button>
                                    <button class="btn btn-ghost" name="action" value="rejectCreate" type="submit">
                                        Отклонить
                                    </button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </section>

    <section class="card">
        <div class="section-header">
            <h2 class="section-title">Правки опубликованных</h2>
            <c:if test="${not empty pendingEdit}">
                <span class="badge"><c:out value="${fn:length(pendingEdit)}"/></span>
            </c:if>
        </div>

        <c:choose>
            <c:when test="${empty pendingEdit}">
                <p class="muted">Очередь правок пуста.</p>
            </c:when>
            <c:otherwise>
                <table class="list table" aria-label="Очередь правок">
                    <thead>
                    <tr>
                        <th>Квест</th>
                        <th>Автор (login)</th>
                        <th>Quest ID</th>
                        <th>Отправлено</th>
                        <th class="col-actions">Действия</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="pe" items="${pendingEdit}">
                        <tr>
                            <td><c:out value="${pe.name}"/></td>
                            <td><code><c:out value="${pe.ownerLogin}"/></code></td>
                            <td><code><c:out value="${pe.questId}"/></code></td>
                            <td><code><c:out value="${pe.submittedAt}"/></code></td>
                            <td class="col-actions td-actions">
                                <form method="post" action="${modAction}" class="actions actions--row">
                                    <input type="hidden" name="id" value="${pe.questId}"/>
                                    <c:url var="previewUrl" value="/quests/moderation/preview">
                                        <c:param name="kind" value="edit"/>
                                        <c:param name="id" value="${pe.questId}"/>
                                    </c:url>
                                    <a class="btn btn-ghost" href="${previewUrl}">Просмотр</a>
                                    <button class="btn btn-primary" name="action" value="approveEdit" type="submit">
                                        Одобрить
                                    </button>
                                    <button class="btn btn-ghost" name="action" value="rejectEdit" type="submit">
                                        Отклонить
                                    </button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </section>

</div>
</body>
</html>