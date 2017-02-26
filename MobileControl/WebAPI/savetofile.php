<?php
array_map('unlink', glob("uploads/*"));
if (isset($_FILES['myFile'])) {
// Example:
move_uploaded_file($_FILES['myFile']['tmp_name'], "uploads/" . $_FILES['myFile']['name']);
echo 'successful';
}
?>
