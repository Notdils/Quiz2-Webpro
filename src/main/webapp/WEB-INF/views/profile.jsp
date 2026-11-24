<ui:layout title="Profile">
    <ui:header title="Profile" eyebrow="${h:label(user.role)}" description="Update your name, email address, and password."/>

    <form method="post" action="${ctx}/profile" class="card form-card" novalidate>
        <ui:csrf/>
        <div class="card-head"><div><h2>Personal information</h2></div></div>
        <div class="card-body form-grid">
            <ui:input name="name" label="Name" value="${f.name}" required="true" maxlength="255" autocomplete="name"/>
            <ui:input name="email" label="Email address" type="email" value="${f.email}" required="true" autocomplete="email"/>
        </div>
        <div class="form-actions"><button type="submit" class="btn btn-primary">Save changes</button></div>
    </form>

    <form method="post" action="${ctx}/profile/password" class="card form-card" novalidate>
        <ui:csrf/>
        <div class="card-head"><div><h2>Change password</h2><p>Use a long password you do not use anywhere else.</p></div></div>
        <div class="card-body form-grid">
            <ui:input name="current_password" label="Current password" type="password" required="true" autocomplete="current-password" cls="full"/>
            <ui:input name="password" label="New password" type="password" required="true" autocomplete="new-password"
                      hint="At least 8 characters, with a number and an uppercase letter."/>
            <ui:input name="password_confirmation" label="Confirm new password" type="password" required="true" autocomplete="new-password"/>
        </div>
        <div class="form-actions"><button type="submit" class="btn btn-primary">Update password</button></div>
    </form>

    <form method="post" action="${ctx}/profile/delete" class="card form-card" data-confirm="Delete your account permanently? This cannot be undone." novalidate>
        <ui:csrf/>
        <div class="card-head"><div><h2>Delete account</h2><p>Your records stay in the system without your name. The last admin cannot delete their account.</p></div></div>
        <div class="card-body form-grid">
            <ui:input name="delete_password" label="Current password" type="password" required="true" autocomplete="current-password"/>
        </div>
        <div class="form-actions"><button type="submit" class="btn btn-danger">${h:icon('trash')} Delete my account</button></div>
    </form>
</ui:layout>
