<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Подтверждение перезаписи — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssConfirm" value="/assets/css/confirm.css"/>
    <link rel="stylesheet" href="${cssMain}">
    <link rel="stylesheet" href="${cssConfirm}">
</head>
<body>

<c:url var="savesAction" value="/saves"/>
<c:url var="backToSaves" value="/saves">
    <c:param name="next" value="${next}"/>
    <c:param name="purpose" value="${purpose}"/>
    <c:param name="node" value="${newNodeId}"/>
</c:url>

<main class="container fade-in" role="main">
    <article class="card" aria-labelledby="title">
        <header class="card-header header-inline-space">
            <h1 id="title">Перезаписать слот №<c:out value="${slotIndex + 1}"/></h1>
            <a href="${backToSaves}" class="btn btn-ghost">Назад</a>
        </header>

        <section class="card-body">
            <p class="notice">Вы собираетесь перезаписать выбранный слот. Текущее содержимое будет утрачено.</p>

            <div class="panel overwrite-panel">
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
                    <input type="hidden" name="op" value="confirm"/>
                    <input type="hidden" name="slot" value="${slotIndex}"/>
                    <input type="hidden" name="node" value="${newNodeId}"/>
                    <input type="hidden" name="next" value="${next}"/>
                    <input type="hidden" name="purpose" value="${purpose}"/>
                    <button type="submit" class="btn btn-primary">Перезаписать</button>
                </form>

                <form method="post" action="${savesAction}">
                    <input type="hidden" name="op" value="cancel"/>
                    <input type="hidden" name="slot" value="${slotIndex}"/>
                    <input type="hidden" name="next" value="${next}"/>
                    <input type="hidden" name="purpose" value="${purpose}"/>
                    <input type="hidden" name="node" value="${newNodeId}"/>
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