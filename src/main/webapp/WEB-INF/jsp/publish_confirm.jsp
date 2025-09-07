<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <title>Публикация квеста</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssPublish" value="/assets/css/publish.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssPublish}"/>

    <c:set var="PATH_HOME" value="/"/>
    <c:set var="PATH_PUBLISH" value="/quest/publish"/>
    <c:set var="PATH_GRAPH" value="/quest/graph_svg"/>

    <c:set var="PARAM_QUEST_NAME" value="questName"/>

    <c:set var="SESSION_EDITING_ID" value="editingQuestId"/>
    <c:set var="SESSION_EDITING_NAME" value="editingQuestName"/>

    <c:url var="postUrl" value="${PATH_PUBLISH}"/>
    <c:url var="backUrl" value="${PATH_GRAPH}"/>
    <c:url var="homeUrl" value="${PATH_HOME}"/>

    <c:set var="editingId" value="${sessionScope[SESSION_EDITING_ID]}"/>
    <c:set var="editingName" value="${sessionScope[SESSION_EDITING_NAME]}"/>
    <c:set var="isEditing" value="${not empty editingId}"/>
</head>
<body class="page-publish">

<main class="container fade-in" role="main">
    <article class="card" aria-labelledby="pageTitle">
        <header class="card-header">
            <h1 id="pageTitle">
                <c:choose>
                    <c:when test="${isEditing}">Подтверждение изменений</c:when>
                    <c:otherwise>Публикация квеста</c:otherwise>
                </c:choose>
            </h1>
        </header>

        <section class="card-body">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <c:choose>
                <c:when test="${isEditing}">
                    <p class="notice">
                        Вы редактируете опубликованный квест:
                        <strong>
                            <c:out value="${empty editingName ? 'Без названия' : editingName}"/>
                        </strong>
                        (ID: <c:out value="${editingId}"/>).
                        Текущая версия будет <strong>перезаписана</strong>.
                    </p>

                    <form method="post" action="${postUrl}" class="stack" style="margin-top:12px">
                        <div class="actions" style="justify-content:flex-end">
                            <a class="btn btn-ghost" href="${backUrl}">Назад</a>
                            <a class="btn btn-ghost" href="${homeUrl}">Отмена</a>
                            <button type="submit" class="btn btn-primary">Сохранить изменения</button>
                        </div>
                    </form>
                </c:when>

                <c:otherwise>
                    <p class="notice">
                        Вы собираетесь опубликовать текущий черновик как новый квест.
                        Перед публикацией будет выполнена проверка структуры и ссылок.
                    </p>

                    <form method="post" action="${postUrl}" class="stack" style="margin-top:12px">
                        <label for="questName" class="label" style="margin-bottom:6px">Название квеста</label>
                        <input id="questName" name="${PARAM_QUEST_NAME}" type="text"
                               class="input"
                               value="${param[PARAM_QUEST_NAME]}"
                               placeholder="Например, «Тайна старого маяка»"/>
                        <div class="actions" style="justify-content:flex-end">
                            <a class="btn btn-ghost" href="${backUrl}">Назад</a>
                            <a class="btn btn-ghost" href="${homeUrl}">Отмена</a>
                            <button type="submit" class="btn btn-primary">Опубликовать</button>
                        </div>
                    </form>
                </c:otherwise>
            </c:choose>
        </section>
    </article>
</main>

</body>
</html>