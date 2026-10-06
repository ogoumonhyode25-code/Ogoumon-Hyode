package com.example

import com.example.ui.screens.auth.AuthViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthValidationTest {

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        viewModel = AuthViewModel()
    }

    @Test
    fun `empty fields produce validation errors`() {
        viewModel.register()
        val state = viewModel.registerForm.value

        assertEquals("Le prénom est requis", state.firstNameError)
        assertEquals("Le nom est requis", state.lastNameError)
        assertEquals("Le nom d'utilisateur est requis", state.usernameError)
        assertEquals("L'adresse e-mail est requise", state.emailError)
        assertEquals("Le numéro de téléphone est requis", state.phoneError)
        assertEquals("Le mot de passe est requis", state.passwordError)
        assertEquals("Veuillez accepter les conditions d'utilisation", state.termsError)
    }

    @Test
    fun `invalid email produces error`() {
        viewModel.updateFirstName("Jean")
        viewModel.updateLastName("Dupont")
        viewModel.updateUsername("jeandupont")
        viewModel.updateEmail("invalid-email")
        viewModel.updatePhone("+22997000000")
        viewModel.updatePassword("123456")
        viewModel.updateConfirmPassword("123456")
        viewModel.toggleTerms(true)

        viewModel.register()
        val state = viewModel.registerForm.value

        assertEquals("Format d'e-mail invalide", state.emailError)
    }

    @Test
    fun `short password produces error`() {
        viewModel.updateFirstName("Jean")
        viewModel.updateLastName("Dupont")
        viewModel.updateUsername("jeandupont")
        viewModel.updateEmail("jean@example.com")
        viewModel.updatePhone("+22997000000")
        viewModel.updatePassword("123")
        viewModel.updateConfirmPassword("123")
        viewModel.toggleTerms(true)

        viewModel.register()
        val state = viewModel.registerForm.value

        assertEquals("Minimum 6 caractères requis", state.passwordError)
    }

    @Test
    fun `mismatched passwords produce error`() {
        viewModel.updateFirstName("Jean")
        viewModel.updateLastName("Dupont")
        viewModel.updateUsername("jeandupont")
        viewModel.updateEmail("jean@example.com")
        viewModel.updatePhone("+22997000000")
        viewModel.updatePassword("password123")
        viewModel.updateConfirmPassword("password456")
        viewModel.toggleTerms(true)

        viewModel.register()
        val state = viewModel.registerForm.value

        assertEquals("Les mots de passe ne correspondent pas", state.confirmPasswordError)
    }

    @Test
    fun `unaccepted terms produce error`() {
        viewModel.updateFirstName("Jean")
        viewModel.updateLastName("Dupont")
        viewModel.updateUsername("jeandupont")
        viewModel.updateEmail("jean@example.com")
        viewModel.updatePhone("+22997000000")
        viewModel.updatePassword("password123")
        viewModel.updateConfirmPassword("password123")
        viewModel.toggleTerms(false)

        viewModel.register()
        val state = viewModel.registerForm.value

        assertEquals("Veuillez accepter les conditions d'utilisation", state.termsError)
    }
}
