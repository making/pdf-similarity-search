const searchBtn = document.getElementById('searchBtn');
const searchInput = document.getElementById('searchInput');

const performSearch = () => {
    searchBtn.textContent = "Searching ...";
    searchBtn.disabled = true;
    searchInput.disabled = true;
    const query = searchInput.value;
    if (!query || query === '') {
        return;
    }
    fetch(`/documents?query=${query}`)
        .then(response => response.json())
        .then(data => {
            const resultsTable = document.getElementById('resultsTable');
            resultsTable.innerHTML = '';
            let tableContent = '';
            data.forEach(item => {
                tableContent += `
                <tr>
                    <td><code>${item.file_name}</code></td>
                    <td>${item.page_number}-${item.end_page_number}</td>
                    <td>${item.title}</td>
                    <td>${item.similarity}</td>
                    <td style="width: 75%;white-space: nowrap;"><pre><code>${item.content}</code></pre></td>
                </tr>
            `;
            });
            resultsTable.innerHTML = tableContent;
        })
        .catch(error => {
            console.error('Error:', error);
        })
        .finally(() => {
            searchBtn.textContent = "Search";
            searchBtn.disabled = false;
            searchInput.disabled = false;
        });
};

searchBtn.addEventListener('click', performSearch);
searchInput.addEventListener('keyup', event => {
    if (event.key === 'Enter') {
        performSearch();
    }
});