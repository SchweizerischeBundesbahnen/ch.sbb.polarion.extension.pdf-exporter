<h3>Supported special variables</h3>

<table>
    <thead>
    <tr>
        <th>Variable</th>
        <th>Description</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td><span class="monospace">{{ PROJECT_NAME }}</span></td>
        <td>Project name</td>
    </tr>
    <tr>
        <td><span class="monospace">{{ DOCUMENT_ID }}</span></td>
        <td>Document ID</td>
    </tr>
    <tr>
        <td><span class="monospace">{{ DOCUMENT_TITLE }}</span></td>
        <td>Document title</td>
    </tr>
    <tr>
        <td><span class="monospace">{{ DOCUMENT_REVISION }}</span></td>
        <td>
            If document is displayed in certain revision&nbsp;&ndash; this revision value. Otherwise,
            if a document has custom field <span class="monospace">docRevision</span>&nbsp;&ndash; values of this custom field.
            Otherwise&nbsp;&ndash; value of <span class="monospace">HEAD</span> revision.
        </td>
    </tr>
    <tr>
        <td><span class="monospace">{{ REVISION }}</span></td>
        <td>Document revision</td>
    </tr>
    <tr>
        <td><span class="monospace">{{ BASELINE_NAME }}</span></td>
        <td>Baseline name</td>
    </tr>
    <tr>
        <td><span class="monospace">{{ REVISION_AND_BASELINE_NAME }}</span></td>
        <td>Revision and baseline name</td>
    </tr>
    <tr>
        <td><span class="monospace">{{ PAGE_NUMBER }}</span></td>
        <td>Page counter</td>
    </tr>
    <tr>
        <td><span class="monospace">{{ PAGES_TOTAL_COUNT }}</span></td>
        <td>Total count of pages in the document</td>
    </tr>
    <tr>
        <td><span class="monospace">{{ PRODUCT_NAME }}</span></td>
        <td>Product name, in common case it will be "Polarion"</td>
    </tr>
    <tr>
        <td><span class="monospace">{{ PRODUCT_VERSION }}</span></td>
        <td>Polarion version</td>
    </tr>
    <tr>
        <td><span class="monospace">{{ TIMESTAMP }}</span></td>
        <td>Date and time of PDF generation</td>
    </tr>
    </tbody>
</table>
