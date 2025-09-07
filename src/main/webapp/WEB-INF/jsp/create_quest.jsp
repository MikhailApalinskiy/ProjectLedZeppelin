<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <title>Редактор узла квеста — TextQuest</title>

    <c:url var="CSS_MAIN" value="/assets/css/main.css"/>
    <c:url var="CSS_EDITOR" value="/assets/css/editor.css"/>
    <c:url var="URL_HOME" value="/"/>
    <c:url var="URL_GRAPH" value="/quest/graph_svg"/>
    <c:url var="URL_EDIT" value="/create_quest"/>

    <c:set var="ACTION_REPLACE" value="replaceNode"/>
    <c:set var="ACTION_DELETE" value="deleteNode"/>

    <c:set var="editingId" value="${sessionScope.editingQuestId}"/>
    <c:set var="editingName" value="${sessionScope.editingQuestName}"/>

    <link rel="stylesheet" href="${CSS_MAIN}"/>
    <link rel="stylesheet" href="${CSS_EDITOR}"/>
</head>
<body class="page-editor">

<header class="app-header">
    <div class="app-header__inner">
        <div class="brand">
            <span class="logo" aria-hidden="true"></span>
            <span>TextQuest — Редактор</span>
        </div>

        <div class="header-actions">
            <a class="btn btn-ghost" href="${URL_HOME}">На главную</a>
            <a class="btn" href="${URL_GRAPH}">Показать паутину</a>

            <c:choose>
                <c:when test="${not empty editingId}">
                    <a class="btn btn-primary" href="${URL_GRAPH}">Сохранить</a>
                </c:when>
                <c:otherwise>
                    <a class="btn btn-primary" href="${URL_GRAPH}">Опубликовать</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</header>

<main class="container fade-in" role="main">
    <article class="card">
        <header class="card-header">
            <h1>Редактор узла квеста</h1>
            <c:if test="${not empty editingId}">
                <span class="badge">Редактируется:
                    <c:out value="${empty editingName ? 'Без названия' : editingName}"/>
                    (id: <c:out value="${editingId}"/>)</span>
            </c:if>
        </header>

        <section class="card-body">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>
            <div id="client-alerts"></div>

            <form id="editorForm" method="post" action="${URL_EDIT}" class="stack" enctype="multipart/form-data">

                <div class="editor-controls-row">
                    <div class="id-field">
                        <label for="id">ID узла</label>
                        <div class="id-stepper control-box" aria-label="Выбор ID узла">
                            <button type="button" class="id-stepper__btn" id="idDec" aria-label="Уменьшить ID">−
                            </button>
                            <input class="input id-stepper__input" id="id" name="id" type="number" min="0" step="1"
                                   value="${form_id}"/>
                            <button type="button" class="id-stepper__btn" id="idInc" aria-label="Увеличить ID">+
                            </button>
                        </div>
                    </div>

                    <div class="flagline">
                        <label class="switch control-box" for="final" title="Отметьте, если это завершение квеста">
                            <input id="final" name="final" type="checkbox"
                                   <c:if test="${form_final}">checked</c:if> />
                            <span class="switch__track" aria-hidden="true"><span class="switch__thumb"></span></span>
                            <span class="switch__text">Финальная ветка</span>
                        </label>
                    </div>
                </div>

                <c:set var="currentImage" value="${form_image}"/>
                <input id="imageFile" name="imageFile" type="file" accept="image/*" class="is-hidden"/>

                <c:choose>
                    <c:when test="${not empty currentImage}">
                        <label for="imageFile" class="upload-preview">
                            <img id="imagePreview"
                                 src="<c:url value='${currentImage}'/>"
                                 data-original-src="<c:url value='${currentImage}'/>"
                                 alt="Картинка узла"/>
                        </label>
                    </c:when>
                    <c:otherwise>
                        <label for="imageFile" id="pickArea" class="upload-drop">Нажмите, чтобы выбрать картинку</label>
                        <div class="upload-preview" id="previewWrap">
                            <img id="imagePreview" alt="Картинка узла"/>
                        </div>
                        <small class="muted">Поддерживаются изображения (jpg, png, webp, gif, svg и др.).</small>
                        <div class="callout callout--tip u-mt-10">
                            <div class="callout__icon">💡</div>
                            <div>Чтобы поменять картинку, откройте сохранённую ветку в «Паутине»</div>
                        </div>
                    </c:otherwise>
                </c:choose>

                <div>
                    <label for="text">Основной текст узла</label>
                    <textarea class="textarea" id="text" name="text"><c:out value="${form_text}"/></textarea>
                </div>

                <div id="optionsSection">
                    <label for="options">Варианты (по одному на строке: <em>текст -> nextId</em>)</label>
                    <textarea class="textarea" id="options" name="options"
                              placeholder="Сказать правду -> 12
Соврать -> 13
Уйти молча -> 5"><c:out value="${form_options}"/></textarea>
                    <p class="notice">Допустимы стрелки: <code>-></code>, <code>→</code>, <code>—&gt;</code>, <code>--&gt;</code>,
                        <code>=&gt;</code>.</p>
                </div>

                <div class="actions actions--right">
                    <button class="btn btn-primary" type="submit" name="action" value="${ACTION_REPLACE}">Сохранить
                        ветку
                    </button>
                    <button class="btn btn-danger" type="submit" name="action" value="${ACTION_DELETE}">Удалить ветку
                    </button>
                </div>
            </form>
        </section>
    </article>
</main>

<script>
    (function () {
        var form = document.getElementById('editorForm');
        if (!form) return;

        var host = document.getElementById('client-alerts');
        var fileInput = document.getElementById('imageFile');
        var previewImg = document.getElementById('imagePreview');
        var previewWrap = document.getElementById('previewWrap');
        var pickArea = document.getElementById('pickArea');

        var idInput = document.getElementById('id');
        var btnDec = document.getElementById('idDec');
        var btnInc = document.getElementById('idInc');

        var isDirty = false;

        function ensureUnsavedAlert() {
            if (!host) return null;
            var el = document.getElementById('unsavedAlert');
            if (el) return el;
            host.insertAdjacentHTML('afterbegin',
                '<div id="unsavedAlert" class="alert alert-info alert--compact">' +
                '  <span class="alert-dot" aria-hidden="true"></span>' +
                '  <div class="alert-text">У вас есть несохранённые изменения. Не забудьте <strong>сохранить ветку</strong>.</div>' +
                '</div>'
            );
            return document.getElementById('unsavedAlert');
        }

        function showUnsavedAlert() {
            var el = ensureUnsavedAlert();
            if (el) el.style.display = 'flex';
        }

        function hideUnsavedAlert() {
            var el = document.getElementById('unsavedAlert');
            if (el) el.style.display = 'none';
        }

        function markDirty() {
            if (!isDirty) {
                isDirty = true;
                showUnsavedAlert();
            }
        }

        if (fileInput) {
            fileInput.addEventListener('change', function () {
                if (!fileInput.files || !fileInput.files[0]) return;
                var url = URL.createObjectURL(fileInput.files[0]);
                if (previewImg) {
                    previewImg.src = url;
                    if (previewWrap) previewWrap.style.display = 'block';
                    if (pickArea) pickArea.style.display = 'none';
                }
                markDirty();
            });
        }

        function clamp(n) {
            var v = parseInt(n, 10);
            if (isNaN(v) || v < 0) v = 0;
            return v;
        }

        function step(d) {
            if (!idInput) return;
            var cur = clamp(idInput.value || "0");
            var next = Math.max(0, cur + d);
            idInput.value = next;
            idInput.dispatchEvent(new Event('change', {bubbles: true}));
        }

        if (btnDec) btnDec.addEventListener('click', function () {
            step(-1);
        });
        if (btnInc) btnInc.addEventListener('click', function () {
            step(+1);
        });
        if (idInput) idInput.addEventListener('blur', function () {
            idInput.value = clamp(idInput.value);
        });

        ['id', 'text', 'options', 'final'].forEach(function (id) {
            var el = document.getElementById(id);
            if (!el) return;
            el.addEventListener('input', markDirty);
            el.addEventListener('change', markDirty);
        });

        form.addEventListener('submit', function () {
            isDirty = false;
            hideUnsavedAlert();
            if (previewImg) previewImg.removeAttribute('src');
            if (previewWrap) previewWrap.style.display = 'none';
            if (pickArea) pickArea.style.display = 'block';
        });
    })();
</script>

</body>
</html>