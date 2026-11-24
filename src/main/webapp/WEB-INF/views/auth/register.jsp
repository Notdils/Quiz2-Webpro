<ui:auth title="Create an account">
    <div>
        <span class="eyebrow">Join the team</span>
        <h1>Create an account</h1>
        <p class="muted" style="margin-top:6px">New accounts start as field staff. An admin can change your role later.</p>
    </div>
    <form method="post" action="${ctx}/register" class="grid" novalidate>
        <ui:csrf/>
        <ui:input name="name" label="Full name" value="${f.name}" required="true" maxlength="255" autocomplete="name"/>
        <ui:input name="email" label="Email address" type="email" value="${f.email}" required="true" autocomplete="email"/>
        <ui:input name="password" label="Password" type="password" required="true" autocomplete="new-password"
                  hint="At least 8 characters, with a number and an uppercase letter."/>
        <ui:input name="password_confirmation" label="Confirm password" type="password" required="true" autocomplete="new-password"/>
        <button type="submit" class="btn btn-primary">Create account</button>
    </form>
    <p class="muted">Already have an account? <a href="${ctx}/login">Log in</a></p>
</ui:auth>
