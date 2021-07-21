package me.jerry.example.webflux.constant

class SecurityConstants {

    companion object {

        const val PASSWORD_PREFIX_NOOP = "{noop}"

        // ==================== Roles ====================
        const val ROLE_SYSTEM = "SYSTEM" // system 상에서 정보를 주고받을 때 사용

        const val ROLE_ADMIN = "ADMIN" // dashboard 등을 볼때 사용

        const val ROLE_ACTUATOR = "ACTUATOR" // actuator 관련

        const val ROLE_API_DOCS = "DOCS" // API Document 조회

        const val PROP_USER_SYSTEM = "security.user.system" // system user property

        const val PROP_USER_ADMIN = "security.user.admin" // admin user property

        const val PROP_USER_ACTUATOR = "security.user.actuator" // actuator user property

        const val PROP_USER_DOCS = "security.user.docs" // docs user property

    }
}
