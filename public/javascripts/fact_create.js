let descriptionElem = document.getElementById("description_text")
let refDiv = document.getElementById("hidden_ref_div")
descriptionElem.addEventListener('input', addRefBoxes)
let createdSections = []

function addRefBoxes(event) {
    let content = event.target.value;
    content.match(/\[\d+\]/g)?.forEach(match => {
        if (document.getElementById(`ref_${match}_field`) == null && !createdSections.includes(`ref_${match}_field`)) {
            var dl = document.createElement("dl")
            dl.id = `ref_${match}_field`
            var dt = document.createElement("dt")
            var dd = document.createElement("dd")
            var label = document.createElement("label");
            label.for = `ref_${match}`;
            let text = document.createTextNode(`Reference link for ${match}`);
            label.appendChild(text);
            var input = document.createElement("input");
            input.type = "text";
            input.id = `ref_${match}`;
            input.name = `References${match}`;
            input.className = "form-control";
            dt.appendChild(label);
            dd.appendChild(input);
            dl.appendChild(dt);
            dl.appendChild(dd);
            refDiv.appendChild(dl);
            createdSections.push(dl.id);
        }
    });
    let contentTags = content.match(/\[\d+\]/g)?.map(match => `ref_${match}_field`);
    let deletedSections = contentTags != null ? createdSections.filter(s => !contentTags.includes(s)) : createdSections;
    if (deletedSections.length > 0) {
        deletedSections.forEach(del => {
            let toDelete = document.getElementById(del);
            toDelete.parentNode.removeChild(toDelete);
            let index = createdSections.indexOf(del);
            createdSections.splice(index, 1);
        })
    }
}