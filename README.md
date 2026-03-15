# Spring Boot Admit Card Generator

A simple and professional Spring Boot backend application that generates PDF admit cards from HTML templates using iText 7.

## Features
- **Professional Layout**: A4 size, clean design with proper tables and borders.
- **Dynamic Content**: Uses placeholders to inject student data into the HTML template.
- **PDF Generation**: Converts HTML to high-quality PDF using `html2pdf`.
- **Optional Digital Signing**: Can sign generated PDFs with a PKCS#12 (`.p12`/`.pfx`) certificate so Adobe can validate the output.
- **REST API**: Simple POST endpoint to receive data and return the generated file.

## Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

## Setup & Installation

1. **Clone or Download** the project to your local machine.
2. **Navigate** to the project directory:
   ```bash
   cd admitcard-generator
   ```
3. **Build** the project:
   ```bash
   mvn clean install
   ```
4. **Run** the application:
   ```bash
   mvn spring-boot:run
   ```
   The application will start on `http://localhost:8080`.

## API Usage

### Generate Admit Card

- **URL**: `http://localhost:8080/api/admitcard/generate`
- **Method**: `POST`
- **Content-Type**: `application/json`

### Example Request Body

Copy and paste this JSON into Postman or your preferred API client:

```json
{
    "name": "Jane Doe",
    "roll": "2024CS001",
    "reg": "REG-2024-0501",
    "course": "B.Tech Computer Science",
    "examName": "Final Semester Examination 2024",
    "collegeName": "Institute of Technology & Science",
    "subject": "Advanced Data Structures",
    "date": "15th May 2024",
    "time": "10:00 AM - 01:00 PM",
    "room": "Block A, Room 304"
}
```

### Example cURL Command

```bash
curl -X POST http://localhost:8080/api/admitcard/generate \
-H "Content-Type: application/json" \
-d '{"name": "Jane Doe", "roll": "2024CS001", "reg": "REG-2024-0501", "course": "B.Tech CA", "examName": "Final Exam 2024", "collegeName": "Tech University", "subject": "Java Programming", "date": "20-May-2024", "time": "10:00 AM - 1:00 PM", "room": "Hall 5"}'
```

### Response

The API will return a JSON response with the success status and the path to the generated PDF.

```json
{
    "success": true,
    "message": "Admit Card generated successfully!",
    "downloadUrl": ".\\generated\\AdmitCard_2024CS001_20240510_123045.pdf"
}
```

## Output

Generated PDF files are saved in the `generated` folder within the project directory.

## Adobe Verification

To get an Adobe "Signature Valid" result, the PDF must be signed with a real private key and a certificate chain that Adobe trusts.

Set these environment variables before starting the app:

```bash
PDF_SIGNATURE_ENABLED=true
PDF_SIGNATURE_KEYSTORE_PATH=/absolute/path/to/certificate.p12
PDF_SIGNATURE_KEYSTORE_PASSWORD=your-keystore-password
PDF_SIGNATURE_KEY_PASSWORD=your-key-password
PDF_SIGNATURE_ALIAS=your-key-alias
```

Notes:
- If the certificate is not trusted by Adobe on the viewing machine, Acrobat may still show the signature as untrusted even though the PDF was signed correctly.
- The app now signs PDFs it generates and signs verification artifacts separately instead of rewriting the original signed bytes in-place.
