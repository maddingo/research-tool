document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("form");
    const input = form.querySelector("input[name='prompt']");
    const responseDiv = document.getElementById("chatresponse");
    const responseCard = document.getElementById("chatresponse-container");

    form.addEventListener("submit", function (event) {
        event.preventDefault(); // Prevent the form from reloading the page

        const prompt = input.value;

        if (prompt.trim() === "") {
            responseDiv.textContent = "Please enter a prompt.";
            return;
        }

        fetch(`/api/chat/${encodeURIComponent(prompt)}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error("Network response was not ok");
                }
                return response.json();
            })
            .then(data => {
                responseDiv.textContent = data.response; // Update the div with the response
                responseCard.classList.remove("w3-hide"); // Show the response card
            })
            .catch(error => {
                console.error("Error:", error);
                responseDiv.textContent = "An error occurred while processing your request.";
            });
    });
});