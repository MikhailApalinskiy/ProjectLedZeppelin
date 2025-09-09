<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Подтверждение перезаписи — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssConfirm" value="/assets/css/confirm.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssConfirm}"/>
</head>
<body>

<c:set var="PATH_SAVES" value="/saves"/>
<c:set var="P_NEXT" value="next"/>
<c:set var="P_PURPOSE" value="purpose"/>
<c:set var="P_NODE" value="node"/>
<c:set var="P_CUSTOM" value="custom"/>
<c:set var="P_SLOT" value="slot"/>
<c:set var="P_OP" value="op"/>
<c:set var="customResolved" value="${not empty param.custom ? param.custom : requestScope.custom}"/>
<c:set var="OP_CONFIRM" value="confirm"/>
<c:set var="OP_CANCEL" value="cancel"/>

<c:url var="savesAction" value="${PATH_SAVES}"/>
<c:url var="backToSaves" value="${PATH_SAVES}">
    <c:param name="${P_NEXT}" value="${next}"/>
    <c:param name="${P_PURPOSE}" value="${purpose}"/>
    <c:param name="${P_NODE}" value="${newNodeId}"/>
    <c:if test="${not empty customResolved}">
        <c:param name="${P_CUSTOM}" value="${customResolved}"/>
    </c:if>
</c:url>

<main class="container fade-in" role="main">
    <article class="card" aria-labelledby="title">
        <header class="card-header header-inline-space">
            <h1 id="title">Перезаписать слот №<c:out value="${slotIndex + 1}"/></h1>
            <a href="${backToSaves}" class="btn btn-ghost">Назад</a>
        </header>

        <section class="card-body">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <p class="notice">Вы собираетесь перезаписать выбранный слот. Текущее содержимое будет утрачено.</p>

            <div class="panel overwrite-panel" role="region" aria-label="Сравнение содержимого слота">
                <div class="compare-block">
                    <h2>Было</h2>
                    <p>Узел #<c:out value="${oldNodeId}"/> — <c:out value="${oldNodeTitle}"/></p>
                </div>
                <div class="compare-block">
                    <h2>Станет</h2>
                    <p>Узел #<c:out value="${newNodeId}"/> — <c:out value="${newNodeTitle}"/></p>
                </div>
            </div>

            <div class="actions confirm-actions">
                <form method="post" action="${savesAction}">
                    <input type="hidden" name="${P_OP}" value="${OP_CONFIRM}"/>
                    <input type="hidden" name="${P_SLOT}" value="${slotIndex}"/>
                    <input type="hidden" name="${P_NODE}" value="${newNodeId}"/>
                    <input type="hidden" name="${P_NEXT}" value="${next}"/>
                    <input type="hidden" name="${P_PURPOSE}" value="${purpose}"/>
                    <c:if test="${not empty customResolved}">
                        <input type="hidden" name="${P_CUSTOM}" value="${customResolved}"/>
                    </c:if>
                    <button type="submit" class="btn btn-primary">Перезаписать</button>
                </form>

                <form method="post" action="${savesAction}">
                    <input type="hidden" name="${P_OP}" value="${OP_CANCEL}"/>
                    <input type="hidden" name="${P_SLOT}" value="${slotIndex}"/>
                    <input type="hidden" name="${P_NEXT}" value="${next}"/>
                    <input type="hidden" name="${P_PURPOSE}" value="${purpose}"/>
                    <input type="hidden" name="${P_NODE}" value="${newNodeId}"/>
                    <c:if test="${not empty customResolved}">
                        <input type="hidden" name="${P_CUSTOM}" value="${customResolved}"/>
                    </c:if>
                    <button type="submit" class="btn btn-ghost">Отмена</button>
                </form>
            </div>

            <div class="linkrow single-linkrow">
                <a class="btn btn-ghost" href="${backToSaves}">Вернуться к слотам</a>
            </div>
        </section>
    </article>
</main>
</body>
</html>