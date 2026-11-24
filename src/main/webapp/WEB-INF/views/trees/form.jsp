<ui:layout title="${title}">
    <ui:header title="${title}" back="${empty f.id ? '/trees' : '/trees/' += f.id}" backLabel="${empty f.id ? 'Trees' : 'Back to tree'}"/>
    <form method="post" action="${ctx}/trees${empty f.id ? '' : '/' += f.id}" enctype="multipart/form-data" class="card form-card" novalidate>
        <ui:csrf/>
        <div class="card-body form-grid">
            <ui:input name="code" label="Code" value="${f.code}" maxlength="30" placeholder="T-0001"
                      hint="${empty f.id ? 'Leave blank to generate the next code automatically.' : ''}"/>
            <ui:select name="status" label="Status" required="true">
                <c:forEach var="o" items="${treeStatuses}"><option value="${o.value}" ${h:sel(o.value, f.status)}>${o.label}</option></c:forEach>
            </ui:select>
            <ui:select name="tree_type_id" label="Tree type" required="true" placeholder="Choose a tree type">
                <c:forEach var="o" items="${treeTypes}"><option value="${o.id}" ${h:sel(o.id, f.tree_type_id)}>${h:e(o.name)} (${h:e(o.category_name)})</option></c:forEach>
            </ui:select>
            <ui:select name="block_id" label="Block" required="true" placeholder="Choose a block">
                <c:forEach var="o" items="${blocks}"><option value="${o.id}" ${h:sel(o.id, f.block_id)}>${h:e(o.name)}</option></c:forEach>
            </ui:select>
            <ui:input name="planted_on" label="Planted on" type="date" value="${f.planted_on}" required="true"/>
            <ui:input name="height_cm" label="Height (cm)" type="number" step="0.01" min="0" value="${f.height_cm}"/>

            <c:set var="photoError" value="${errors.photo}"/>
            <div class="field full ${not empty photoError ? 'has-error' : ''}">
                <label for="f-photo">Photo</label>
                <div class="file-drop">
                    <c:choose>
                        <c:when test="${f.has_photo == true}"><img id="photo-preview" src="${ctx}/trees/${f.id}/photo" alt="Current photo"></c:when>
                        <c:otherwise><img id="photo-preview" alt="Selected photo preview" hidden></c:otherwise>
                    </c:choose>
                    <div class="grid" style="gap:6px">
                        <input id="f-photo" name="photo" type="file" accept="image/jpeg,image/png,image/webp" data-preview="photo-preview">
                        <p class="hint">JPG, PNG, or WebP, up to 2 MB.</p>
                        <c:if test="${f.has_photo == true}">
                            <label class="check"><input type="checkbox" name="remove_photo" value="1"> Remove the current photo</label>
                        </c:if>
                    </div>
                </div>
                <c:if test="${not empty photoError}"><p class="error">${h:e(photoError)}</p></c:if>
            </div>

            <ui:textarea name="notes" label="Notes" value="${f.notes}" placeholder="Condition, location details, or anything worth remembering."/>
        </div>
        <div class="form-actions">
            <a class="btn btn-ghost" href="${ctx}${empty f.id ? '/trees' : '/trees/' += f.id}">Cancel</a>
            <button type="submit" class="btn btn-primary">${empty f.id ? 'Create tree' : 'Save changes'}</button>
        </div>
    </form>
</ui:layout>
