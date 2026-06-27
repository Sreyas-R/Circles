export const API_BASE_URL = 'http://localhost:8080';

export const ERROR_MESSAGES = {
  'Email_Used_Already': 'This email is already registered.',
  'Username_Used_Already': 'This username is already taken.',
  'Invalid_Password': 'Password must be at least 8 characters long and contain numbers, uppercase, lowercase, and special characters.',
  'INVALID_CREDENTIALS': 'Invalid username or password.',
  'LOGIN_FAILURE': 'Login failed. Please try again.',
  'SERVER_ERROR': 'A server error occurred. Please try again later.',
  'Empty_File': 'Selected file is empty.',
  'Invalid_File': 'Unsupported file type. Please upload JPEG, PNG, PDF, or DOC/DOCX.',
  'File_Too_Large': 'File is too large (max limit is 10MB).',
  'S3_UPLOAD_FAILED': 'Failed to upload file to storage.',
  'INTERNAL_SERVER_ERROR': 'Internal server error occurred.'
};

export function getErrorMessage(result) {
  if (!result || !result.errorMessage) return 'An unknown error occurred';
  return ERROR_MESSAGES[result.errorMessage] || result.errorMessage;
}
